# SleepRecorder 🌙

Prototype d'application Android développée en Kotlin. 
# 🌙 SleepRecorder - Guide d'Utilisation

Bienvenue dans **SleepRecorder**, une application Android native conçue pour capturer les bruits de la nuit (ronflements, paroles, bruits environnants).

Ce document explique comment installer et utiliser l'application au quotidien.

---

## Installation

1. Téléchargez le fichier **`app-debug.apk`** fourni dans les livrables (ou compilez le projet via Android Studio).
2. Transférez le fichier sur votre smartphone Android.
3. Ouvrez le fichier pour l'installer. *(Remarque : il se peut que votre téléphone vous demande d'autoriser l'installation d'applications issues de sources inconnues).*

---

## Comment utiliser l'application ?

### 1. Préparer sa nuit
* Ouvrez l'application **SleepRecorder**.
* Appuyez sur le bouton central **"COMMENCER LA NUIT"**.
* Lors de la première utilisation, l'application vous demandera **deux permissions obligatoires** :
  * **Microphone :** Pour pouvoir écouter les bruits.
  * **Notifications :** Pour afficher que le service tourne en arrière-plan (requis par Android).
* Une fois les permissions accordées, le bouton devient rouge (Néon) et affiche **"ARRÊTER"**.

### 2. La phase d'endormissement (30 minutes)
Pour éviter d'enregistrer vos mouvements lorsque vous cherchez le sommeil, **le microphone ne s'active pas immédiatement**. 
* L'application entre d'abord dans une phase de pause de 30 minutes. 
* Vous pouvez verrouiller votre téléphone et éteindre l'écran en toute tranquillité. Le service restera actif en arrière-plan.

### 3. L'enregistrement intelligent
Une fois la phase d'endormissement terminée, l'application écoute silencieusement. 
* Si le volume sonore dépasse un certain seuil (ex: toux, voix forte), l'application enregistre un clip audio de **10 secondes**.
* Le fichier est sauvegardé dans la mémoire de l'application.

### 4. Le lendemain matin
* Déverrouillez votre téléphone et rouvrez l'application.
* Appuyez sur le gros bouton rouge **"ARRÊTER"** pour couper le microphone et le service en arrière-plan.
* Appuyez sur le bouton en bas de l'écran : **"Voir les enregistrements"**.

---

## 🎧 Gérer ses enregistrements

Sur l'écran de la liste des enregistrements, vous retrouverez toutes les captures sonores de vos nuits précédentes.

* ** Écouter :** Appuyez simplement sur une ligne pour lancer la lecture audio. L'icône de lecture deviendra verte. Appuyez à nouveau pour mettre en pause.
* ** Télécharger :** Par défaut, les fichiers sont cachés pour votre vie privée. Si vous souhaitez conserver un fichier ou le partager, appuyez sur l'icône de partage (à droite de l'enregistrement). Le fichier sera copié dans le dossier public **`Téléchargements/SleepRecorder`** de votre téléphone.
* ** Tout supprimer :** Si vous souhaitez faire le ménage manuellement, un bouton rouge "Tout supprimer" en haut à droite permet d'effacer définitivement tous les enregistrements d'un seul coup.

> **💡 Note sur le stockage :** Pour éviter de saturer la mémoire de votre téléphone, l'application effectue un nettoyage automatique. À chaque fois que vous lancez une nouvelle nuit, les fichiers vieux de plus de 24 heures sont supprimés automatiquement.

---

## 🛡️ Confidentialité
* Cette application fonctionne **100% hors-ligne**. 
* Aucun son ni fichier n'est envoyé sur des serveurs externes.
* Les fichiers bruts sont stockés dans le bac à sable privé de l'application (Scoped Storage) et ne sont accessibles qu'à vous.

## Fonctionnalités 🚀
* **Écoute intelligente :** Utilisation de `AudioRecord` en arrière-plan pour détecter les seuils sonores.
* **Service persistant :** Implémentation d'un `Foreground Service` avec `WakeLock` pour garantir le fonctionnement de l'application l'écran éteint.
* **Sauvegarde sécurisée :** Enregistrement des données (`MediaRecorder`) dans le stockage interne privé de l'application.
* **Export public :** Utilisation de l'API `MediaStore` pour permettre à l'utilisateur d'exporter ses fichiers audio vers le dossier public "Téléchargements".
* **Nettoyage automatique :** Gestion automatique de l'espace de stockage avec suppression des fichiers obsolètes.

## Technologies utilisées 🛠️
* **UI :** Jetpack Compose (Material 3, Dark Theme natif, animations personnalisées).
* **Concurrence :** Kotlin Coroutines (`Dispatchers.IO`) pour la gestion asynchrone du moteur audio sans bloquer le Main Thread.
* **Audio :** API `AudioRecord` (PCM) et `MediaRecorder` (AAC/m4a).
