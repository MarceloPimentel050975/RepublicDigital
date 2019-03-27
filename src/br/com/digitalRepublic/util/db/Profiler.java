package br.com.digitalRepublic.util.db;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Profiling b�sico para aplica��es.
 */
public final class Profiler {

    private static final Map<String,ProfileEntry> profileData =
        new HashMap<String,ProfileEntry>();
    private static final Map<String,LocalStack<Execution>> executionData =
        new HashMap<String,LocalStack<Execution>>();

    private static long startTime;
    private static long pending;

    private Profiler() { }

    private static final class LocalStack<E> {
        private static final long serialVersionUID = 1756190233898028668L;

        private ThreadLocal<LinkedList<E>> stack = new ThreadLocal<LinkedList<E>>() {
            protected java.util.LinkedList<E> initialValue() {
                return new LinkedList<E>();
            }
        };

        public void push(E item) {
            stack.get().addLast(item);
        }

        public E pop() {
            return stack.get().removeLast();
        }

        public E peek() {
            return stack.get().getLast();
        }

        public boolean isEmpty() {
            return stack.get().isEmpty();
        }

        public int size() {
            return stack.get().size();
        }

    }

    private static final class ProfileEntry {
        private long calls;
        private long accumulated;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = Long.MIN_VALUE;

        public void addExecution(Execution execution) {
            calls++;
            final long duration = execution.getDuration();
            accumulated += duration;
            if (duration < minDuration) {
                minDuration = duration;
            }
            if (duration > maxDuration) {
                maxDuration = duration;
            }
        }

        public long getCalls() {
            return calls;
        }

        public long getAccumulated() {
            return accumulated;
        }

        public long getMaxDuration() {
            return maxDuration;
        }

        public long getMinDuration() {
            return minDuration;
        }

        public long getAverage() {
            return accumulated / calls;
        }

    }

    private static final class Execution {
        private long startTime;
        private long duration;

        public Execution() {
            startTime = System.currentTimeMillis();
            pending++;
        }

        public void stop() {
            duration = System.currentTimeMillis() - startTime;
            pending--;
        }

        public void stop(long forceTime) {
            duration = forceTime - startTime;
            pending--;
        }

        public long getDuration() {
            return duration;
        }

    }

    public static final void start(String key) {
        synchronized (executionData) {
            LocalStack<Execution> stack = executionData.get(key);
            if (stack == null) {
                executionData.put(key, stack = new LocalStack<Execution>());
            }
            stack.push(new Execution());
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
        }
    }

    public static final void stop(String key) {
        long callTime = System.currentTimeMillis();
        LocalStack<Execution> stack = executionData.get(key);
        if (stack == null || stack.isEmpty()) {
            System.out.println("Profiling n�o foi feito corretamente: " + key);
            return;
        }
        Execution execution = stack.pop();
        execution.stop(callTime);

        ProfileEntry entry = profileData.get(key);
        if (entry == null) {
            profileData.put(key, entry = new ProfileEntry());
        }
        entry.addExecution(execution);
    }

    public static String results() {
        long uptime = System.currentTimeMillis() - startTime;
        StringBuffer buffer = new StringBuffer();
        buffer.append("###### Profile Data ######\n");
        buffer.append("method call").append('\t');
        buffer.append("# of calls").append('\t');
        buffer.append("accumulated time").append('\t');
        buffer.append("average duration").append('\t');
        buffer.append("max duration").append('\t');
        buffer.append("min duration");
        for (Entry<String,ProfileEntry> entry : profileData.entrySet()) {
            buffer.append('\n');
            buffer.append(entry.getKey()).append('\t');
            buffer.append(entry.getValue().getCalls()).append('\t');
            buffer.append(entry.getValue().getAccumulated()).append('\t');
            buffer.append(entry.getValue().getAverage()).append('\t');
            buffer.append(entry.getValue().getMaxDuration()).append('\t');
            buffer.append(entry.getValue().getMinDuration());
        }
        buffer.append("\nTotal runtime: ").append(uptime);
        buffer.append('\n');
        buffer.append("###### End of profile data ######");
        return buffer.toString();
    }

    public static boolean hasPendingExecutions() {
        return pending == 0;
    }

    public static void reset() {
        synchronized (executionData) {
            executionData.clear();
            profileData.clear();
        }
    }

}
