import { spawnSync, execSync } from 'child_process';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const nativeDir = path.join(__dirname, '..', 'src-native');
const outputDir = path.join(nativeDir, 'build');

console.log('[Build] Starting native service build...');
console.log('[Build] Native directory:', nativeDir);
console.log('[Build] Output directory:', outputDir);

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const objDir = path.join(outputDir, 'obj');
if (!fs.existsSync(objDir)) {
  fs.mkdirSync(objDir, { recursive: true });
}

const vsInstallDir = 'D:\\Microsoft Visual Studio\\Professional';
const vcvarsallPath = path.join(vsInstallDir, 'VC', 'Auxiliary', 'Build', 'vcvarsall.bat');

console.log('[Build] Looking for vcvarsall.bat:', vcvarsallPath);

if (!fs.existsSync(vcvarsallPath)) {
  console.error('[Build] ERROR: vcvarsall.bat not found!');
  process.exit(1);
}

const batchContent = `@echo off
setlocal
echo [Build] Setting up MSVC environment...
call "${vcvarsallPath}" x64
if errorlevel 1 (
  echo [Build] ERROR: Failed to set up MSVC environment
  exit /b 1
)
echo [Build] Environment set up successfully
echo [Build] Starting compilation...

cd /d "${nativeDir}"

echo [Build] Building NativeService.exe...
cl.exe /EHsc /std:c++17 /Fe:"${outputDir}\\NativeService.exe" /Fo:"${objDir}\\" /nologo /W4 /O2 /I"${nativeDir}" /I"${nativeDir}\\nlohmann" /I"${nativeDir}\\media-monitor" native-service.cpp media-monitor/media-monitor.cpp /link /nologo psapi.lib powrprof.lib ole32.lib oleaut32.lib winmm.lib ws2_32.lib winhttp.lib uuid.lib kernel32.lib user32.lib gdi32.lib advapi32.lib shell32.lib comctl32.lib /OUT:"${outputDir}\\NativeService.exe"

if errorlevel 1 (
  echo [Build] ERROR: NativeService.exe compilation failed
  exit /b 1
)
echo [Build] NativeService.exe built successfully

echo [Build] Building NativeServiceDLL.dll...
cl.exe /EHsc /std:c++17 /LD /Fe:"${outputDir}\\NativeServiceDLL.dll" /Fo:"${objDir}\\" /nologo /W4 /O2 /I"${nativeDir}" /I"${nativeDir}\\nlohmann" /I"${nativeDir}\\media-monitor" /DNATIVE_SERVICE_DLL_EXPORTS native-service-dll.cpp media-monitor/media-monitor.cpp taskbar/taskbar-utils.cpp /link /nologo /DLL psapi.lib powrprof.lib ole32.lib oleaut32.lib winmm.lib ws2_32.lib winhttp.lib uuid.lib kernel32.lib user32.lib gdi32.lib advapi32.lib shell32.lib comctl32.lib /OUT:"${outputDir}\\NativeServiceDLL.dll"

if errorlevel 1 (
  echo [Build] ERROR: NativeServiceDLL.dll compilation failed
  exit /b 1
)
echo [Build] NativeServiceDLL.dll built successfully

echo [Build] Build completed successfully
endlocal`;

const batchFile = path.join(outputDir, 'build-native.bat');
fs.writeFileSync(batchFile, batchContent);

console.log('[Build] Created batch file:', batchFile);
console.log('[Build] Running build...');

const result = spawnSync('cmd.exe', ['/c', batchFile], {
  stdio: 'inherit',
  cwd: nativeDir,
  shell: false
});

if (result.status !== 0) {
  console.error('[Build] Build failed with exit code:', result.status);
  process.exit(1);
}

console.log('[Build] Native service build completed successfully');
console.log('[Build] Output file:', path.join(outputDir, 'NativeService.exe'));
console.log('[Build] Output file:', path.join(outputDir, 'NativeServiceDLL.dll'));