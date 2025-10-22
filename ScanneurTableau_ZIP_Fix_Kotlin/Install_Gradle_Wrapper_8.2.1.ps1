# ==========================================================================================
# Script : Install_Gradle_Wrapper_8.2.1.ps1
# Auteur : ChatGPT pour Emmanuel
# Compatibilité : PowerShell 3.0+
# Projet : C:\Users\Emmanuel\Desktop\SCAN\ScanneurTableau_ZIP_Fix_Kotlin
# ==========================================================================================

Write-Host "`n=== Exécution du script d’installation Gradle 8.2.1 ===`n" -ForegroundColor Cyan

# === Confirmation de signature locale =====================================================
$answer = Read-Host "Souhaitez-vous signer ce script avant exécution ? (O/N)"
if ($answer -match '^[Oo]$') {
    try {
        if (-not (Get-Command New-SelfSignedCertificate -ErrorAction SilentlyContinue)) {
            Write-Host "⚠️  Impossible de créer un certificat (fonction non disponible sur cette version). Le script continuera sans signature." -ForegroundColor Yellow
        }
        else {
            $cert = New-SelfSignedCertificate -CertStoreLocation Cert:\CurrentUser\My -Subject "GradleWrapperInstaller" -KeyUsage DigitalSignature -Type CodeSigningCert
            Set-AuthenticodeSignature -FilePath $MyInvocation.MyCommand.Definition -Certificate $cert | Out-Null
            Write-Host "✅ Script signé localement." -ForegroundColor Green
        }
    }
    catch {
        Write-Host "⚠️  Échec de la signature locale : $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# === Paramètres projet ====================================================================
$ProjectDir = "C:\Users\Emmanuel\Desktop\SCAN\ScanneurTableau_ZIP_Fix_Kotlin"
$RepoName   = "ScanneurTableau_ZIP_Fix_Kotlin"
$CommitMsg  = "Ajout Gradle Wrapper 8.2.1"

# === Vérification du projet et de Git =====================================================
if (-not (Test-Path $ProjectDir)) {
    Write-Host "❌ Le dossier projet n’existe pas : $ProjectDir" -ForegroundColor Red
    exit 1
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Git n’est pas installé ou non détecté dans le PATH." -ForegroundColor Red
    exit 1
}

Set-Location $ProjectDir
if (-not (Test-Path ".git")) {
    Write-Host "⚠️  Aucun dépôt Git détecté. Initialisation en cours..." -ForegroundColor Yellow
    git init | Out-Null
    Write-Host "✅ Dépôt Git initialisé." -ForegroundColor Green
}

# === Téléchargement du Gradle Wrapper =====================================================
Write-Host "⬇️  Téléchargement du Gradle Wrapper 8.2.1..." -ForegroundColor Cyan
$gradleJarUrl = "https://repo.gradle.org/gradle/libs-releases-local/org/gradle/gradle-wrapper/8.2.1/gradle-wrapper-8.2.1.jar"
$gradleWrapperDir = Join-Path $ProjectDir "gradle\wrapper"
New-Item -ItemType Directory -Force -Path $gradleWrapperDir | Out-Null
Invoke-WebRequest -Uri $gradleJarUrl -OutFile (Join-Path $gradleWrapperDir "gradle-wrapper.jar") -UseBasicParsing

# === Fichier gradle-wrapper.properties ====================================================
$gradleProps = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
$gradleProps | Out-File (Join-Path $gradleWrapperDir "gradle-wrapper.properties") -Encoding UTF8

# === Scripts gradlew et gradlew.bat =======================================================
$gradlew = @"
#!/usr/bin/env sh
APP_HOME=`dirname "$0"`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE=java
exec "$JAVA_EXE" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
"@
$gradlew | Out-File (Join-Path $ProjectDir "gradlew") -Encoding UTF8
$gradlewBat = @"
@ECHO OFF
SET APP_HOME=%~dp0
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%\bin\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
"@
$gradlewBat | Out-File (Join-Path $ProjectDir "gradlew.bat") -Encoding OEM

# === README_WRAPPER.txt ==================================================================
$readme = @"
Gradle Wrapper 8.2.1 (Giraffe)
==============================
Fichiers créés :
- gradlew
- gradlew.bat
- gradle/wrapper/gradle-wrapper.jar
- gradle/wrapper/gradle-wrapper.properties

Utilisation :
1. Compilation locale :
   gradlew.bat assembleDebug
2. Commit & Push :
   git add gradlew gradlew.bat gradle/wrapper README_WRAPPER.txt
   git commit -m "$CommitMsg"
   git push
"@
$readme | Out-File (Join-Path $ProjectDir "README_WRAPPER.txt") -Encoding UTF8

# === Commit & Push =======================================================================
Write-Host "📤 Commit et push vers GitHub..." -ForegroundColor Cyan
git add gradlew gradlew.bat gradle/wrapper README_WRAPPER.txt
git commit -m $CommitMsg | Out-Null
git push | Out-Null

# === Fin =================================================================================
Write-Host "`n✅ Le Gradle Wrapper a été installé et poussé vers le dépôt GitHub : $RepoName" -ForegroundColor Green
# ==========================================================================================
