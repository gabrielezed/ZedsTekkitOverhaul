package zed.tekkitoverhaul.lunar;

/** Read access and subscriptions for one session. A detached session returns null. */
public interface LunarService {
    LunarSnapshot snapshot();

    /** Subscribe on the authority thread. Duplicate registrations have no effect. */
    void subscribe(Listener listener);
    void unsubscribe(Listener listener);

    interface Listener {
        void onLunarEvent(LunarEvent event);
    }
}
