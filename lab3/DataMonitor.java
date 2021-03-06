import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class DataMonitor implements Watcher, StatCallback {
    ZooKeeper zk;
    String znode;
    boolean dead;
    DataMonitorListener listener;

    public DataMonitor(ZooKeeper zk, String znode, DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.listener = listener;
        watch();
    }

    public void watch() {
        zk.exists(znode, true, this, null);
        int below = this.watchChildren(znode);

        if (below == -1) {
            System.out.println("Node " + znode + " does not exist");
        } else if (below == 1) {
            System.out.println("There is 1 node below " + znode);
        } else {
            System.out.println("There are " + Integer.toString(below) + " node(s) below " + znode);
        }
    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case Expired:
                    // It's all over
                    dead = true;
                    listener.closing(Code.SessionExpired);
                    break;
                case SyncConnected:
                    // In this particular example we don't need to do anything
                    // here - watches are automatically re-registered with
                    // server and any watches triggered while the client was
                    // disconnected will be delivered (in order of course)
                    break;
            }
        } else {
            if (path != null) {
                // Something has changed on the node, let's find out
                watch();
            }
        }
    }
    @Override
    public void processResult(int num, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (num) {
            case Code.NoAuth:
                dead = true;
                listener.closing(num);
                return;
            case Code.Ok:
                exists = true;
                break;
            case Code.NoNode:
                exists = false;
                break;
            case Code.SessionExpired:
            default:
                // Retry errors
                zk.exists(znode, true, this, null);
                return;
        }

        listener.exists(exists);
    }

    private int countChildren(List<String> children, String path){
        int below = 0;
        for (String child : children) {
            below += 1 + this.watchChildren(path + "/" + child);
        }
        return below;
    }

    private int watchChildren(String path) {
        int count = 0;
        try {
            List<String> children = zk.getChildren(path, true);
            count = countChildren(children, path);
        } catch (KeeperException e) {
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return count;
    }
}