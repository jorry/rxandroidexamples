package kurtis.rx.androidexamples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import kurtis.rx.androidexamples.mode.Result;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class Example8Activity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createObservables();

    }

    private void createObservables() {
        Observable<Result> listObservable = Observable.just(new Result());
        listObservable.observeOn(AndroidSchedulers.mainThread());
        listObservable.lift(new MyOperator()).flatMap(new Func1<Result, Observable<?>>() {
            @Override
            public Observable<?> call(Result result) {
                return contactMerge();
            }
        }).subscribe();
    }

    public Observable contactMerge(){
        return  Observable.create(new Observable.OnSubscribe<Result>() {

            @Override
            public void call(Subscriber<? super Result> subscriber) {
                Log.d("lift", "contactMerge");

            }
        });

    }

}
