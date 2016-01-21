LicenseManager.java is released under the MIT License.
And LicenseManager.java uses modules(java files) which is released under the Apache License, Version 2.0. 

Update:
 2016-01-21: In-app Billing APIが結果を返すまでwaitしてnewを完了する処理の組み込み
   onCreateでnewして、onResumeでisLicensePurchased()メソッドにて購入状態を判定するという作りにしていたが、onCreateからonResumeが走るまでにIn-app Billing APIが結果を返しきる保証は何もないので、判定を誤るという可能性があった。この不具合の修正。
   JDeferred（https://github.com/jdeferred/jdeferred）を利用していることに留意のこと。
