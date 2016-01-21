/* 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Takahiro Shinchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 */

package jp.blogspot.save-dep-mukku;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.google.common.base.Function;

import org.jdeferred.android.AndroidDeferredManager;

/**
 * Created by SHINCHI, Takahiro on 2015/05/06.
 */
public class LicenseManager {
    private static final String TAG = "LicenseManager";

    /* 使用にあたり設定する項目 */

    // ライセンス購入の任意の識別子。購入後の正当性を確かめるために使用する
    private static final String payload = "適当な識別子 ex) LicensePayload";

    // リクエストコード
    private static final int RC_REQUEST = 適当なリクエストコード ex) 10001;

    // 購入アイテム（ライセンス） * Google Play Developer Consoleより取得
    private static final String license = "アプリのライセンスID";

    // Google Play License Key * Google Play Developer Consoleより取得
    private static final String GOOGLE_PLAY_LICENSE_KEY = "アプリのライセンスキー";

    /* 設定項目はここまで */


    // ライセンス購入フラグ
    private boolean licensePurchaseFlag = false;

    // ヘルパーオブジェクト
    private IabHelper mHelper;

    // アプティビティ
    private Activity activity;

    // アクティビティのUI更新用のコールバック関数
    private Function callback;

    public LicenseManager(Activity activity, Function callback) {
        this.activity = activity;
        this.callback = callback;

        String base64EncodedPublicKey = GOOGLE_PLAY_LICENSE_KEY;
        mHelper = new IabHelper(activity, base64EncodedPublicKey);

        Log.d(TAG, mHelper.toString());

        // ライセンス購入セットアップ開始
        AndroidDeferredManager dm = new AndroidDeferredManager();
        try {
            dm.when(() -> {
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            Log.d(TAG, "セットアップ失敗 結果: " + result);
                            return;
                        }
                        // オブジェクトが生成されていない
                        if (mHelper == null) return;
                        Log.d(TAG, "セットアップ成功。 購入情報照会へ");
                        mHelper.queryInventoryAsync(mGotInventoryListener);
                    }
                });
            }).done(result -> {
                // do nothing
            }).fail(tr -> {
                tr.printStackTrace();
            }).waitSafely();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // オブジェクトが生成されていない
            if (result == null) {
                Log.d(TAG, "IabResultがnull");
                return;
            }
            // 購入情報照会失敗
            if (result.isFailure()) {
                Log.d(TAG, "購入情報照会失敗 結果 : " + result);
                return;
            }
            //　購読情報があるかどうか？
            Purchase subscriptionPurchase = inventory.getPurchase(license);
            // 購読購入が済んでいる場合はフラグを変更
            licensePurchaseFlag = (subscriptionPurchase != null && verifyDeveloperPayload(subscriptionPurchase));
            Log.d(TAG, "購入状況をセットした。購入ステータス： " + String.valueOf(licensePurchaseFlag));

            // callbackをコールしてUIを更新
            callback.apply(null);
        }
    };

    public boolean isLicensePurchased() {
        return licensePurchaseFlag;
    }

    public IabHelper getIabHelper() {
        return mHelper;
    }

    // ライセンス購入ボタン
    public void purchaseLicense(View v) {
        Log.d(TAG, "ライセンス購入ボタンクリック");
        if (licensePurchaseFlag) {
            Log.d(TAG, "ライセンス購入済み。何も処理せずreturn");
            return;
        } else {
            Log.d(TAG, "ライセンス未購入。購入処理に入る。");
            mHelper.launchPurchaseFlow(activity, license, RC_REQUEST, mPurchaseFinishedListener, payload);
        }
    }

    // 購入処理が完了した後に呼ばれる処理
    public IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // オブジェクトが生成されていない場合は終了
            if (mHelper == null) {
                Log.d(TAG, "IabHelperが生成されていません。");
                return;
            }

            // エラー時の処理
            if (result.isFailure()) {
                Log.d(TAG, "購入に失敗してます。");
                return;
            }

            // 購入商品がlicenseの場合
            if (purchase.getSku().equals(license)) {
                Log.d(TAG, "ライセンスの購入が完了しました。");
                // 購入した商品に対応する処理
            }

        }
    };

    // 識別子をチェックする
    public boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        Log.d(TAG, "in verify, payload is : " + payload);
        return LicenseManager.payload.equals(payload);
    }

    // onDestroyで必ず呼ぶこと
    public void destroy() {
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }
}
