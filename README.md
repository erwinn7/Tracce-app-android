# 🏝️ Tracce - Application Android Native (V1)

Application mobile Android native développée en **Kotlin** et **Jetpack Compose**, compagnon de la PWA Tracce pour l'exploration du patrimoine corse, la validation de quêtes géolocalisées et la synchronisation multi-plateforme.

---

## 📱 Fonctionnalités clés

* **Exploration & Quêtes géolocalisées** : Carte interactive des sites remarquables de Corse (Ajaccio, Bastia, Corte, etc.) avec radar de proximité GPS.
* **Architecture Locale d'abord (Offline-First)** :
  * Base de données locale **Room** (version 2 avec migration) servant de source de vérité locale.
  * Synchronisation bidirectionnelle avec **Google Cloud Firestore** (Pattern Outbox & réconciliation en arrière-plan via **WorkManager**).
* **Validation de Quêtes avec Caméra** :
  * Prise de photo in-app pour attester de la visite d'un lieu.
  * Stockage interne sécurisé des clichés.
* **Cycle de vie Android rigoureux (Compose & SavedState)** :
  * Résistance totale aux changements de configuration (rotation d'écran).
  * Résistance à la mort de processus par l'OS en arrière-plan (`am kill`) via le `savedInstanceState` Bundle.
  * Réinitialisation propre à neuf lors de la fermeture volontaire par l'utilisateur (swipe-away des applications récentes).
* **Mode Administrateur** : Ajout déterministe de nouveaux lieux au catalogue avec capture GPS automatique et synchronisation Firestore.
* **Multi-utilisateurs & Carnet de bord** : Isolation des points, badges et quêtes validées selon le profil de l'explorateur connecté.

---

## 📦 Téléchargement direct (APK)

L'APK prêt à l'installation est directement disponible dans le dépôt :
👉 **[`apk/Tracce-v1.apk`](apk/Tracce-v1.apk)**

---

## 🛠️ Configuration & Compilation

### Prérequis
* Android Studio Ladybug / Meerkat (ou version récente)
* Android SDK (API 34/35/37)
* JDK 17 ou 21

### Installation & Configuration locale
1. Clonez le dépôt :
   ```bash
   git clone https://github.com/erwinn7/Tracce-app-android.git
   cd Tracce-app-android
   ```
2. Créez votre fichier `local.properties` à partir du modèle fourni :
   ```bash
   cp local.properties.example local.properties
   ```
3. Renseignez le chemin de votre SDK et votre clé Firebase :
   ```properties
   sdk.dir=/Users/VOTRE_NOM/Library/Android/sdk
   FIREBASE_API_KEY=VOTRE_CLE_FIREBASE
   ```
4. Lancez la compilation :
   ```bash
   ./gradlew assembleDebug
   ```
