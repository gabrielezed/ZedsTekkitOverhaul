using System;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class BackgroundMinecraftNative
{
    [StructLayout(LayoutKind.Sequential)]
    private struct SecurityAttributes
    {
        public int Size;
        public IntPtr Descriptor;
        public int Inherit;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct StartupInfo
    {
        public int Size;
        public string Reserved, Desktop, Title;
        public int X, Y, Width, Height, XChars, YChars, Fill, Flags;
        public short ShowWindow, ReservedSize;
        public IntPtr ReservedData, Input, Output, Error;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct ProcessInfo
    {
        public IntPtr Process, Thread;
        public int ProcessId, ThreadId;
    }

    [DllImport("user32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr CreateDesktop(string name, IntPtr device, IntPtr mode,
        int flags, int access, ref SecurityAttributes security);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool CloseDesktop(IntPtr desktop);

    [DllImport("user32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr OpenDesktop(string name, int flags, bool inherit, int access);

    private delegate bool WindowCallback(IntPtr window, IntPtr state);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool EnumDesktopWindows(IntPtr desktop, WindowCallback callback, IntPtr state);

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr window, out int pid);

    [DllImport("user32.dll")]
    private static extern bool IsWindowVisible(IntPtr window);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool PostMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern IntPtr GetThreadDesktop(uint thread);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SetThreadDesktop(IntPtr desktop);

    [DllImport("kernel32.dll")]
    private static extern uint GetCurrentThreadId();

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool CreateProcess(string executable, StringBuilder command, IntPtr processSecurity,
        IntPtr threadSecurity, bool inherit, int flags, IntPtr environment, string directory,
        ref StartupInfo startup, out ProcessInfo process);

    [DllImport("kernel32.dll")]
    private static extern bool CloseHandle(IntPtr handle);

    public static Process Start(string name, string executable, string arguments, string directory, string stdout, string stderr)
    {
        var security = new SecurityAttributes { Size = Marshal.SizeOf(typeof(SecurityAttributes)), Inherit = 1 };
        uint thread = GetCurrentThreadId();
        IntPtr original = GetThreadDesktop(thread);
        const int DesktopAccess = 0x01ff;
        IntPtr desktop = CreateDesktop(name, IntPtr.Zero, IntPtr.Zero, 0, DesktopAccess, ref security);
        if (desktop == IntPtr.Zero) throw new Win32Exception();
        try
        {
            if (GetThreadDesktop(thread) == desktop && !SetThreadDesktop(original)) throw new Win32Exception();
            var startup = new StartupInfo { Size = Marshal.SizeOf(typeof(StartupInfo)), Desktop = name };
            var command = new StringBuilder("\"" + executable + "\" " + arguments);
            using (var output = OpenLog(stdout))
            using (var error = OpenLog(stderr))
            using (var input = output == null ? null : new FileStream("NUL", FileMode.Open, FileAccess.Read,
                FileShare.ReadWrite | FileShare.Inheritable))
            {
                if (output != null)
                {
                    startup.Flags = 0x100; // STARTF_USESTDHANDLES
                    startup.Input = input.SafeFileHandle.DangerousGetHandle();
                    startup.Output = output.SafeFileHandle.DangerousGetHandle();
                    startup.Error = error.SafeFileHandle.DangerousGetHandle();
                }
                ProcessInfo info;
                const int CreateNoWindow = 0x08000000;
                if (!CreateProcess(executable, command, IntPtr.Zero, IntPtr.Zero, true, CreateNoWindow,
                    IntPtr.Zero, directory, ref startup, out info)) throw new Win32Exception();
                try
                {
                    var process = Process.GetProcessById(info.ProcessId);
                    process.EnableRaisingEvents = true;
                    return process;
                }
                finally
                {
                    CloseHandle(info.Thread);
                    CloseHandle(info.Process);
                }
            }
        }
        finally { CloseDesktop(desktop); }
    }

    public static IntPtr GameWindow(string name, int pid)
    {
        const int DesktopReadObjects = 1;
        IntPtr desktop = OpenDesktop(name, 0, false, DesktopReadObjects);
        if (desktop == IntPtr.Zero) return IntPtr.Zero;
        try
        {
            IntPtr result = IntPtr.Zero;
            EnumDesktopWindows(desktop, delegate(IntPtr window, IntPtr state)
            {
                int owner;
                GetWindowThreadProcessId(window, out owner);
                // Prism can host LWJGL in an AWT frame rather than a top-level LWJGL window.
                if (owner != pid || !IsWindowVisible(window)) return true;
                result = window;
                return false;
            }, IntPtr.Zero);
            return result;
        }
        finally { CloseDesktop(desktop); }
    }

    public static bool CloseGameWindow(string name, int pid)
    {
        const uint CloseWindowMessage = 0x0010;
        IntPtr window = GameWindow(name, pid);
        return window != IntPtr.Zero && PostMessage(window, CloseWindowMessage, IntPtr.Zero, IntPtr.Zero);
    }

    private static FileStream OpenLog(string path)
    {
        return String.IsNullOrEmpty(path) ? null : new FileStream(path, FileMode.Create, FileAccess.Write,
            FileShare.Read | FileShare.Inheritable);
    }
}
