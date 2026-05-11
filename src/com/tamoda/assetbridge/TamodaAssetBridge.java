package com.tamoda.assetbridge;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceRequest;
// [TAMBAHAN] Import view dan viewgroup untuk membedah VerticalArrangement
import android.view.View;
import android.view.ViewGroup;
import java.io.InputStream;
import java.io.IOException;
import android.os.Build;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

@DesignerComponent(
    version = 3,
    description = "Ekstensi Injektor Aset Hybrid untuk TAMODA App. Support Custom WebView (VerticalArrangement).",
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

    // [PERBAIKAN] Parameter diubah dari WebViewer menjadi AndroidViewComponent
    // Agar bisa menerima komponen apa saja (termasuk VerticalArrangement)
    @SimpleFunction(description = "Memasang pencegat lalu lintas jaringan ke CustomWebView (lewat VerticalArrangement).")
    public void StartCapture(final AndroidViewComponent webViewContainer, String dummyDomain) {
        if (dummyDomain != null && !dummyDomain.isEmpty()) {
            this.targetDomain = dummyDomain;
            if (!this.targetDomain.endsWith("/")) {
                this.targetDomain += "/";
            }
        }

        try {
            View view = webViewContainer.getView();
            WebView webView = null;

            // Logika Pintar: Cek apakah yang dikirim WebViewer bawaan atau VerticalArrangement
            if (view instanceof WebView) {
                webView = (WebView) view;
            } else if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                // Menggeledah isi VerticalArrangement untuk mencari Custom WebView
                for (int i = 0; i < vg.getChildCount(); i++) {
                    if (vg.getChildAt(i) instanceof WebView) {
                        webView = (WebView) vg.getChildAt(i);
                        break;
                    }
                }
            }

            // Jika WebView tidak ditemukan di dalam komponen yang bos masukkan
            if (webView == null) {
                Log.e("TamodaAssetBridge", "Error: Tidak ada WebView di dalam komponen yang dipilih!");
                return;
            }

            // Pasang pencegat ke WebView yang berhasil ditemukan
            webView.setWebViewClient(new WebViewClient() {
                
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String url = request.getUrl().toString();
                        if (url.startsWith(targetDomain)) {
                            WebResourceResponse response = handleAssetIntercept(url);
                            if (response != null) {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Access-Control-Allow-Origin", "*");
                                response.setResponseHeaders(headers);
                                return response;
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (url.startsWith(targetDomain)) {
                        WebResourceResponse response = handleAssetIntercept(url);
                        if (response != null) {
                            return response; 
                        }
                    }
                    return super.shouldInterceptRequest(view, url);
                }
            });
        } catch (Exception e) {
            Log.e("TamodaAssetBridge", "Gagal memasang injektor: " + e.getMessage());
        }
    }

    private WebResourceResponse handleAssetIntercept(String url) {
        try {
            String fileName = url.replace(this.targetDomain, "");
            
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }
            if (fileName.contains("#")) {
                fileName = fileName.substring(0, fileName.indexOf("#"));
            }

            String mimeType = "image/png"; 
            String lowerFileName = fileName.toLowerCase();
            
            if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (lowerFileName.endsWith(".gif")) mimeType = "image/gif";
            else if (lowerFileName.endsWith(".webp")) mimeType = "image/webp";
            else if (lowerFileName.endsWith(".svg")) mimeType = "image/svg+xml";
            else if (lowerFileName.endsWith(".mp3")) mimeType = "audio/mpeg";
            else if (lowerFileName.endsWith(".css")) mimeType = "text/css";
            else if (lowerFileName.endsWith(".js")) mimeType = "application/javascript";
            else if (lowerFileName.endsWith(".json")) mimeType = "application/json";

            InputStream inputStream = context.getAssets().open(fileName);
            return new WebResourceResponse(mimeType, "UTF-8", inputStream);
        } catch (IOException e) {
            Log.e("TamodaAssetBridge", "Aset lokal tidak ditemukan: " + url);
            return null; 
        }
    }
}
