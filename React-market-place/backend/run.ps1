$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceRoot = Join-Path $backendRoot 'src\main\java'
$outputRoot = Join-Path $backendRoot 'target\classes'

if (Test-Path $outputRoot) {
    Remove-Item $outputRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null

$sources = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d $outputRoot $sources
java -cp $outputRoot com.example.ecommerce.EcommerceBackendApplication

