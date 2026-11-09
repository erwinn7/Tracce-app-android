package corsica.erwinn.tracce.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import corsica.erwinn.tracce.data.model.Quest

class WebAppInterface(private val onSelectQuest: (String) -> Unit) {
    @JavascriptInterface
    fun onPinClick(questId: String) {
        onSelectQuest(questId)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    quests: List<Quest>,
    completedIds: Set<String>,
    selectedQuestId: String?,
    onSelectQuest: (Quest) -> Unit,
    modifier: Modifier = Modifier,
    onWebViewReady: ((WebView) -> Unit)? = null
) {
    val htmlContent = remember(quests, completedIds, selectedQuestId) {
        generateLeafletHtml(quests, completedIds, selectedQuestId)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("LeafletWebView", "${consoleMessage?.message()} [ligne ${consoleMessage?.lineNumber()}]")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(
                            "if (window.map) { window.map.invalidateSize(true); }",
                            null
                        )
                    }
                }
                addJavascriptInterface(WebAppInterface { qId ->
                    post {
                        val quest = quests.find { it.id == qId }
                        if (quest != null) onSelectQuest(quest)
                    }
                }, "AndroidBridge")
                tag = htmlContent
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                onWebViewReady?.invoke(this)
            }
        },
        update = { webView ->
            // Ne recharge que si le contenu HTML change réellement (évite les scintillements blancs sur scroll)
            if (webView.tag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

private fun generateLeafletHtml(
    quests: List<Quest>,
    completedIds: Set<String>,
    selectedQuestId: String?
): String {
    val completedQuests = quests.filter { it.id in completedIds }
    val polylineCoords = completedQuests.joinToString(",") { "[${it.lat}, ${it.lng}]" }

    val markersJs = StringBuilder()
    quests.forEach { q ->
        val isDone = q.id in completedIds
        val isSelected = q.id == selectedQuestId
        val pinSize = if (isSelected) 42 else 34
        val pinHeight = if (isSelected) 48 else 40
        val needleY = pinHeight - 3
        val headRadius = if (isSelected) 13 else 11
        val headY = 14
        val headX = pinSize / 2

        if (isDone) {
            markersJs.append("""
                var pinHtml = `
                    <div style="cursor:pointer;" title="${q.name.replace("'", "\\'")}">
                      <svg width="$pinSize" height="$pinHeight" viewBox="0 0 $pinSize $pinHeight" fill="none">
                        <ellipse cx="${headX + 3}" cy="${needleY}" rx="6" ry="3" fill="rgba(0,0,0,0.35)" />
                        <polygon points="${headX - 1.5},${headY + headRadius - 2} ${headX + 1.5},${headY + headRadius - 2} ${headX},${needleY}" fill="#cbd5e1" stroke="#475569" stroke-width="0.5"/>
                        <circle cx="${headX}" cy="${headY}" r="${headRadius}" fill="#dc2626" stroke="#7f1d1d" stroke-width="1.2"/>
                        <ellipse cx="${headX - headRadius * 0.35}" cy="${headY - headRadius * 0.35}" rx="${headRadius * 0.35}" ry="${headRadius * 0.2}" fill="#ffffff" opacity="0.8" />
                      </svg>
                    </div>
                `;
                var icon = L.divIcon({
                    html: pinHtml,
                    className: 'custom-pin',
                    iconSize: [$pinSize, $pinHeight],
                    iconAnchor: [$headX, $needleY]
                });
                var m = L.marker([${q.lat}, ${q.lng}], { icon: icon }).addTo(map);
                m.on('click', function() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onPinClick('${q.id}');
                    }
                });
            """.trimIndent())
        } else {
            markersJs.append("""
                var discHtml = `
                    <div style="background:white; border:2px dashed #94a3b8; border-radius:50%; width:16px; height:16px; display:flex; align-items:center; justify-content:center; box-shadow:0 1px 3px rgba(0,0,0,0.2); cursor:pointer;">
                      <div style="width:6px; height:6px; background:#94a3b8; border-radius:50%;"></div>
                    </div>
                `;
                var icon = L.divIcon({
                    html: discHtml,
                    className: 'unvisited-pin',
                    iconSize: [16, 16],
                    iconAnchor: [8, 8]
                });
                var m = L.marker([${q.lat}, ${q.lng}], { icon: icon }).addTo(map);
                m.on('click', function() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onPinClick('${q.id}');
                    }
                });
            """.trimIndent())
        }
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="file:///android_asset/leaflet/leaflet.css" />
            <script src="file:///android_asset/leaflet/leaflet.js"></script>
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #f8faf9;
                }
                #map {
                    width: 100%;
                    height: 100%;
                    min-height: 460px;
                }
                .leaflet-control-attribution { font-size: 8px !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    center: [42.15, 9.15],
                    zoom: 8,
                    minZoom: 6,
                    maxZoom: 17,
                    zoomControl: false
                });

                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                L.control.zoom({ position: 'topright' }).addTo(map);

                ${if (completedQuests.size > 1) """
                    L.polyline([$polylineCoords], {
                        color: '#dc2626',
                        weight: 2.5,
                        opacity: 0.8,
                        dashArray: '5, 5'
                    }).addTo(map);
                """.trimIndent() else ""}

                $markersJs

                window.recenterCorsica = function() {
                    map.setView([42.15, 9.15], 8, { animate: true });
                };

                window.fitPins = function() {
                    var bounds = L.latLngBounds([$polylineCoords]);
                    if (bounds.isValid()) {
                        map.fitBounds(bounds, { padding: [30, 30], maxZoom: 12, animate: true });
                    }
                };

                function forceResize() {
                    if (window.map) {
                        window.map.invalidateSize(true);
                    }
                }
                window.addEventListener('load', function() {
                    forceResize();
                    setTimeout(forceResize, 100);
                    setTimeout(forceResize, 350);
                    setTimeout(forceResize, 800);
                });
                window.addEventListener('resize', forceResize);
            </script>
        </body>
        </html>
    """.trimIndent()
}
