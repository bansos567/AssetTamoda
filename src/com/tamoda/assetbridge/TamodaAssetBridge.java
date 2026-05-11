package com.tamoda.assetbridge;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceRequest;
import java.io.InputStream;
import java.io.IOException;
import android.os.Build;
import android.util.Log;

@DesignerComponent(
    version = 2,
    description = "Injektor Aset Cerdas untuk TAMODA App. Bisa dipasang ke Wadah (Vertical Arrangement) untuk mencari WebView di dalamnya secara otomatis.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png"
)
@SimpleObject(external = true)
public class TamodaAssetBridge extends AndroidNonvisibleComponent {
    private Context context;
    private String targetDomain = "https://app.tamoda/";

    public TamodaAssetBridge(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Injektor Otomatis. Masukkan komponen wadah (misal: VerticalArrangement) dan domain dummy.")
    public void StartCapture(final AndroidViewComponent webViewContainer, String dummyDomain) {
        if (dummyDomain != null && !dummyDomain.isEmpty()) {
            this.targetDomain = dummyDomain;
            if (!this.targetDomain.endsWith("/")) {
                this.targetDomain += "/";
            }
        }

        // Ambil View dari wadah yang dimasukkan bos
        final View containerView = webViewContainer.getView();

        // Jalankan di UI Thread agar tidak crash saat menyentuh WebView
        form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Cari WebView secara mendalam (Recursive)
                final WebView realWebView = findWebViewRecursive(containerView);

                if (realWebView != null) {
                    realWebView.setWebViewClient(new WebViewClient() {
                        
                        // Interceptor untuk HP Modern (Lollipop+)
                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                String url = request.getUrl().toString();
                                WebResourceResponse response = handleAssetIntercept(url);
                                if (response != null) return response;
                            }
                            return super.shouldInterceptRequest(view, request);
                        }

                        // Interceptor untuk HP Jadul
                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                            WebResourceResponse response = handleAssetIntercept(url);
                            if (response != null) return response;
                            return super.shouldInterceptRequest(view, url);
                        }
                    });
                    Log.i("TamodaAssetBridge", "Berhasil menemukan WebView di dalam wadah dan memasang Interceptor!");
                } else {
                    Log.e("TamodaAssetBridge", "Gagal! Tidak ditemukan komponen WebView di dalam wadah yang bos berikan.");
                }
            }
        });
    }

    // Mesin Pencari WebView: Bongkar wadah sampai ketemu WebView
    private WebView findWebViewRecursive(View view) {
        if (view instanceof WebView) {
            return (WebView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebViewRecursive(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private WebResourceResponse handleAssetIntercept(String url) {
        if (url.startsWith(this.targetDomain)) {
            try {
                String fileName = url.replace(this.targetDomain, "");
                String mimeType = getMimeType(fileName);

                InputStream inputStream = context.getAssets().open(fileName);
                return new WebResourceResponse(mimeType, "UTF-8", inputStream);
            } catch (IOException e) {
                Log.e("TamodaAssetBridge", "Aset lokal tidak ketemu bos: " + url);
                return null;
            }
        }
        return null;
    }

    private String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        return "image/png";
    }
}
