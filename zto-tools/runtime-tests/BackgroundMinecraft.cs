using System;
using System.ComponentModel;
using System.Runtime.InteropServices;
using System.Text;

// Read-only. Sample from the interactive user's session and input desktop.
public static class RuntimeInputState
{
    [StructLayout(LayoutKind.Sequential)]
    public struct Point { public int X, Y; }

    [StructLayout(LayoutKind.Sequential)]
    public struct Rect { public int Left, Top, Right, Bottom; }

    [StructLayout(LayoutKind.Sequential)]
    private struct GuiThreadInfo
    {
        public int Size, Flags;
        public IntPtr Active, Focus, Capture, MenuOwner, MoveSize, Caret;
        public Rect CaretRect;
    }

    public sealed class Sample
    {
        public string WindowStation, ThreadDesktop, InputDesktop, ForegroundDesktop;
        public uint ForegroundPid, FocusPid, CapturePid;
        public Point Cursor;
        public Rect Clip, VirtualScreen;
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr OpenInputDesktop(uint flags, bool inherit, uint access);
    [DllImport("user32.dll")]
    private static extern bool CloseDesktop(IntPtr desktop);
    [DllImport("user32.dll")]
    private static extern IntPtr GetProcessWindowStation();
    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr GetThreadDesktop(uint thread);
    [DllImport("kernel32.dll")]
    private static extern uint GetCurrentThreadId();
    [DllImport("user32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool GetUserObjectInformation(IntPtr handle, int index, StringBuilder value,
        int capacityInBytes, out int needed);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetCursorPos(out Point point);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetClipCursor(out Rect rect);
    [DllImport("user32.dll")]
    private static extern int GetSystemMetrics(int index);
    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr window, out uint pid);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetGUIThreadInfo(uint thread, ref GuiThreadInfo info);

    private static string Name(IntPtr handle)
    {
        if (handle == IntPtr.Zero) throw new Win32Exception();
        var name = new StringBuilder(256);
        int needed;
        if (!GetUserObjectInformation(handle, 2, name, name.Capacity * 2, out needed)) throw new Win32Exception();
        return name.ToString();
    }

    public static Sample Read()
    {
        var result = new Sample();
        result.WindowStation = Name(GetProcessWindowStation());
        result.ThreadDesktop = Name(GetThreadDesktop(GetCurrentThreadId()));
        IntPtr input = OpenInputDesktop(0, false, 1); // DESKTOP_READOBJECTS
        if (input == IntPtr.Zero) throw new Win32Exception();
        try { result.InputDesktop = Name(input); }
        finally { CloseDesktop(input); }
        if (result.ThreadDesktop != result.InputDesktop)
            throw new InvalidOperationException("Sample from the current input desktop.");
        IntPtr foreground = GetForegroundWindow();
        uint thread = GetWindowThreadProcessId(foreground, out result.ForegroundPid);
        if (thread != 0)
        {
            result.ForegroundDesktop = Name(GetThreadDesktop(thread));
            var info = new GuiThreadInfo { Size = Marshal.SizeOf(typeof(GuiThreadInfo)) };
            if (!GetGUIThreadInfo(thread, ref info)) throw new Win32Exception();
            GetWindowThreadProcessId(info.Focus, out result.FocusPid);
            GetWindowThreadProcessId(info.Capture, out result.CapturePid);
        }
        if (!GetCursorPos(out result.Cursor) || !GetClipCursor(out result.Clip)) throw new Win32Exception();
        result.VirtualScreen.Left = GetSystemMetrics(76);
        result.VirtualScreen.Top = GetSystemMetrics(77);
        result.VirtualScreen.Right = result.VirtualScreen.Left + GetSystemMetrics(78);
        result.VirtualScreen.Bottom = result.VirtualScreen.Top + GetSystemMetrics(79);
        return result;
    }
}
