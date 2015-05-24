package jp.blogspot.save-dep-mukku;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import jp.blogspot.save-dep-mukku.util.IabHelper;
import jp.blogspot.save-dep-mukku.util.IabResult;
import jp.blogspot.save-dep-mukku.util.Inventory;
import jp.blogspot.save-dep-mukku.util.Purchase;

/**
 * Created by SHINCHI, Takahiro on 2015/05/06.
 */
public class LicenseManager {
    private static final String TAG = "LicenseManager";

    // ライセンス購入の任意の識別子。購入後の正当性を確かめるために使用する
    private static final String payload = "適当な識別子 ex) LicensePayload";

    // リクエストコード
    private static final int RC_REQUEST = 適当なリクエストコード ex) 10001;

    // 購入アイテム（ライセンス） * Google Play Developer Consoleより取得
    private static final String license = "アプリのライセンスID";

    // ライセンス購入フラグ
    private boolean licensePurchaseFlag = false;

    // ヘルパーオブジェクト
    private IabHelper mHelper;

    // アプティビティ
    private Activity activity;

    public LicenseManager(Activity activity) {
        this.activity = activity;

        String base64EncodedPublicKey = activity.getString(R.string.google_play_key);
        mHelper = new IabHelper(activity, base64EncodedPublicKey);

        Log.d("mHelper:", mHelper.toString());

        // ライセンス購入セットアップ開始
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "セットアップ失敗 結果: " + result);
                    return;
                }
                // オブジェクトが生成されていない
                if (mHelper == null) return;
                Log.d(TAG, "セットアップ成功。 購入情報照会");
                // 購入情報照会
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            // オブジェクトが生成されていない
            if (mHelper == null) return;

            // 購入情報照会失敗
            if (result.isFailure()) {
                Log.d(TAG, "購入情報照会失敗 結果 : " + result);
                return;
            }

            //　購読情報があるかどうか？
            Purchase subscriptionPurchase = inventory.getPurchase(license);

            // 購読購入が済んでいる場合はフラグを変更
            licensePurchaseFlag = (subscriptionPurchase != null && verifyDeveloperPayload(subscriptionPurchase));
        }
    };

    public boolean isLicensePurchased() {
        return this.licensePurchaseFlag;
    }

    public IabHelper getIabHelper() {
        return this.mHelper;
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
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // オブジェクトが生成されていない場合は終了
            if (mHelper == null) return;

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
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        Log.d(TAG, "in verify, payload is : " + payload);
        return true;
    }

    // onDestroyで必ず呼ぶこと
    void destroy() {
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    Activity getActivity() {
        return this.activity;
    }

}
