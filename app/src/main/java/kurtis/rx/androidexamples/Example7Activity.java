package kurtis.rx.androidexamples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import kurtis.rx.androidexamples.mode.Result;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class Example7Activity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createObservables();

    }

    private void createObservables() {
        Observable<Result> listObservable = Observable.just(new Result());
        listObservable.observeOn(AndroidSchedulers.mainThread());
        listObservable.lift(new MyOperator()).subscribe(new Subscriber<Result>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Result result) {
                Log.d(",lift","onNext-------scriber  = "+result.isNeedLogin());
            }
        });
    }


}
