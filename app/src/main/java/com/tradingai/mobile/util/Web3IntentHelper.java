package com.tradingai.mobile.util;

import android.content.Intent;
import android.net.Uri;

public final class Web3IntentHelper {
    private Web3IntentHelper() {
    }

    public static Intent createMetaMaskDeepLink(String dappUrl) {
        Uri uri = Uri.parse("https://metamask.app.link/dapp/" + dappUrl);
        return new Intent(Intent.ACTION_VIEW, uri);
    }
}
