package com.artemchep.basics_multithreading;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artemchep.basics_multithreading.cipher.CipherUtil;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<WithMillis<Message>> mList = new ArrayList<>();

    private MessageAdapter mAdapter = new MessageAdapter(mList);

    BlockingQueue queue = new BlockingQueue();
    Thread worker = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d("TAG_MAIN", "Thread started");
            while (true) {
                Runnable task = queue.get();
                task.run();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
        showWelcomeDialog();
        worker.start();
    }

    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("What are you going to need for this task: Thread, Handler.\n" +
                        "\n" +
                        "1. The main thread should never be blocked.\n" +
                        "2. Messages should be processed sequentially.\n" +
                        "3. The elapsed time SHOULD include the time message spent in the queue.")
                .show();
    }

    public void onPushBtnClick(View view) {
        Message message = Message.generate();
        insert(new WithMillis<>(message));
    }

    @UiThread
    public void insert(final WithMillis<Message> message) {
        mList.add(message);
        mAdapter.notifyItemInserted(mList.size() - 1);

        // TODO: Start processing the message (please use CipherUtil#encrypt(...)) here.
        //       After it has been processed, send it to the #update(...) method.

        // How it should look for the end user? Uncomment if you want to see. Please note that
        // you should not use poor decor view to send messages to UI thread.
//        getWindow().getDecorView().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                final Message messageNew = message.value.copy("sample :)");
//                final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, CipherUtil.WORK_MILLIS);
//                update(messageNewWithMillis);
//            }
//        }, CipherUtil.WORK_MILLIS);

        Log.d("TAG_MAIN", "Must be main " +Thread.currentThread().getName());

        final long time = SystemClock.elapsedRealtime();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String cipherMessage = CipherUtil.encrypt(message.value.plainText);
                Log.d("TAG_MAIN", "CipherUtil.encrypt " + CipherUtil.getTime());
                final Message messageNew = message.value.copy(cipherMessage);
                final long elapsedTime = SystemClock.elapsedRealtime() - time;
                Log.d("TAG_MAIN", "elapsedTime" + elapsedTime);
                final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, elapsedTime);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        update(messageNewWithMillis);
                    }
                });
            }
        };

        queue.put(runnable);

    }

    static class BlockingQueue {
        ArrayList<Runnable> tasks = new ArrayList<>();

        public synchronized Runnable get() {
            while (tasks.isEmpty()) {
                try {
                    wait();
                    Log.d("TAG_MAIN", Thread.currentThread().getName() + " thread sleep");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Runnable task = tasks.get(0);
            tasks.remove(task);
            return task;
        }

        public synchronized void put(Runnable task) {
            tasks.add(task);
            Log.d("TAG_MAIN", Thread.currentThread().getName() + "thread up");
            notifyAll();
        }
    }

    @UiThread
    public void update(final WithMillis<Message> message) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).value.key.equals(message.value.key)) {
                mList.set(i, message);
                mAdapter.notifyItemChanged(i);
                return;
            }
        }

        throw new IllegalStateException();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.interrupt();
    }
}
