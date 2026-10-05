#include <windows.h>
#include <stdio.h>

#define APP_NAME L"Victoria II War Analyzer"
#define JAR_NAME L"vickywaranalyzer.jar"

#define PATHSIZE (MAX_PATH * 2)

static void fail(const wchar_t *detail)
{
	wchar_t message[1024];
	_snwprintf(message, 1024, L"%ls\n\n%ls", APP_NAME, detail);
	MessageBoxW(NULL, message, APP_NAME, MB_OK | MB_ICONERROR);
}

static int exists(const wchar_t *path)
{
	return GetFileAttributesW(path) != INVALID_FILE_ATTRIBUTES;
}

/* Folder of the running executable, without the trailing backslash. */
static int executable_folder(wchar_t *folder)
{
	DWORD written = GetModuleFileNameW(NULL, folder, PATHSIZE);
	wchar_t *last;

	if (written == 0 || written >= PATHSIZE) {
		return 0;
	}
	last = wcsrchr(folder, L'\\');
	if (last == NULL) {
		return 0;
	}
	*last = L'\0';
	return 1;
}

/* The bundled runtime first, then JAVA_HOME, then whatever is on the PATH. */
static int find_java(const wchar_t *folder, wchar_t *java)
{
	wchar_t home[PATHSIZE];
	DWORD written;
	DWORD length;

	_snwprintf(java, PATHSIZE, L"%ls\\jre\\bin\\javaw.exe", folder);
	if (exists(java)) {
		return 1;
	}

	home[0] = L'\0';
	written = GetEnvironmentVariableW(L"JAVA_HOME", home, PATHSIZE);
	if (written > 0 && written < PATHSIZE) {
		_snwprintf(java, PATHSIZE, L"%ls\\bin\\javaw.exe", home);
		if (exists(java)) {
			return 1;
		}
	}

	length = SearchPathW(NULL, L"javaw.exe", NULL, PATHSIZE, java, NULL);
	return length > 0 && length < PATHSIZE;
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previous, PWSTR command_line, int show)
{
	wchar_t folder[PATHSIZE];
	wchar_t java[PATHSIZE];
	wchar_t jar[PATHSIZE];
	wchar_t command[PATHSIZE * 2];
	STARTUPINFOW startup;
	PROCESS_INFORMATION process;

	(void) instance;
	(void) previous;
	(void) command_line;
	(void) show;

	if (!executable_folder(folder)) {
		fail(L"No se pudo determinar la carpeta del programa.");
		return 1;
	}

	if (!find_java(folder, java)) {
		fail(L"No se encontro un Java 8 con JavaFX.\n\n"
		     L"Coloca un runtime con JavaFX en la carpeta \"jre\" junto a este "
		     L"archivo, o define JAVA_HOME.");
		return 1;
	}

	_snwprintf(jar, PATHSIZE, L"%ls\\%ls", folder, JAR_NAME);
	if (!exists(jar)) {
		fail(L"No se encontro el archivo vickywaranalyzer.jar junto a este programa.");
		return 1;
	}

	_snwprintf(command, PATHSIZE * 2, L"\"%ls\" -jar \"%ls\"", java, jar);

	ZeroMemory(&startup, sizeof(startup));
	startup.cb = sizeof(startup);
	ZeroMemory(&process, sizeof(process));

	/* The working directory has to stay here, paths.txt is written to it. */
	if (!CreateProcessW(java, command, NULL, NULL, FALSE, 0, NULL, folder, &startup, &process)) {
		fail(L"No se pudo iniciar el analizador.");
		return 1;
	}
	CloseHandle(process.hProcess);
	CloseHandle(process.hThread);
	return 0;
}