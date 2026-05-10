package com.tamoda.assetbridge;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceRequest;
import android.os.Build;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;

@DesignerComponent(
    version = 1,
    description = "Ekstensi Injektor Aset Hybrid untuk TAMODA App. Melayani file langsung dari folder assets APK.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
public class TamodaAssetBridge extends AndroidNonvisibleComponent {
    
    private Context context;
    private String targetDomain = "https://app.tamoda/";

    public TamodaAssetBridge(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    @SimpleFunction(description = "Memasang pencegat lalu lintas jaringan ke WebViewer untuk menyuntikkan aset lokal.")
    public void StartCapture(final WebViewer webViewerComponent, String dummyDomain) {
        if (dummyDomain != null && !dummyDomain.isEmpty()) {
            this.targetDomain = dummyDomain;
            if (!this.targetDomain.endsWith("/")) {
                this.targetDomain += "/";
            }
        }

        try {
            final WebView webView = (WebView) webViewerComponent.getView();
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String url = request.getUrl().toString();
                        WebResourceResponse response = handleAssetIntercept(url);
                        if (response != null) {
                            return response;
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    WebResourceResponse response = handleAssetIntercept(url);
                    if (response != null) {
                        return response;
                    }
                    return super.shouldInterceptRequest(view, url);
                }
            });
        } catch (Exception e) {
            Log.e("TamodaAssetBridge", "Gagal memasang injektor: " + e.getMessage());
        }
    }

    private WebResourceResponse handleAssetIntercept(String url) {
        if (url.startsWith(this.targetDomain)) {
            try {
                String fileName = url.replace(this.targetDomain, "");
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
        return null;
    }
}
