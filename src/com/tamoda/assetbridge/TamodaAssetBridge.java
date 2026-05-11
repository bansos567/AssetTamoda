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
    version = 5,
    description = "Injektor Aset TAMODA V5 - Ultimate Edition (Fixed Path & Timing).",
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

    @SimpleEvent(description = "Laporan status injeksi.")
    public void OnCaptureStatus(boolean success, String message) {
        EventDispatcher.dispatchEvent(this, "OnCaptureStatus", success, message);
    }

    @SimpleFunction(description = "Mulai proses tangkap aset. Gunakan domain dummy yang sama dengan di HTML.")
    public void StartCapture(final Component container, final String dummyDomain) {
        if (dummyDomain != null && !dummyDomain.isEmpty()) {
            this.targetDomain = dummyDomain.endsWith("/") ? dummyDomain : dummyDomain + "/";
        }

        form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View view = null;
                    if (container instanceof AndroidViewComponent) {
                        view = ((AndroidViewComponent) container).getView();
                    }

                    final WebView webView = findWebViewRecursive(view);

                    if (webView != null) {
                        // SET CLIENT BARU
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    return handleAssetIntercept(request.getUrl().toString());
                                }
                                return super.shouldInterceptRequest(view, request);
                            }

                            @Override
                            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                                return handleAssetIntercept(url);
                            }

                            // Tambahan: Pastikan gambar dimuat ulang saat halaman siap
                            @Override
                            public void onPageFinished(WebView view, String url) {
                                super.onPageFinished(view, url);
                            }
                        });
                        
                        // PAKSA WEBVIEW SUPAYA MAU AKSES FILE LOKAL
                        webView.getSettings().setAllowFileAccess(true);
                        webView.getSettings().setAllowContentAccess(true);
                        
                        OnCaptureStatus(true, "WebView Berhasil Disuntik!");
                    } else {
                        OnCaptureStatus(false, "Error: WebView tidak ditemukan di dalam komponen tersebut!");
                    }
                } catch (Exception e) {
                    OnCaptureStatus(false, "Fatal Error: " + e.getMessage());
                }
            }
        });
    }

    private WebView findWebViewRecursive(View view) {
        if (view == null) return null;
        if (view instanceof WebView) return (WebView) view;
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
        // Logika pembersihan URL: Hilangkan 'https://app.tamoda/'
        if (url.startsWith(this.targetDomain)) {
            String fileName = url.replace(this.targetDomain, "");
            
            // Buang tanda tanya (query) jika ada, misal: gambar.png?v=123
            if (fileName.contains("?")) {
                fileName = fileName.split("\\?")[0];
            }

            try {
                InputStream is = context.getAssets().open(fileName);
                String mimeType = getMimeType(fileName);
                // Kembalikan file dari APK ke WebView
                return new WebResourceResponse(mimeType, "UTF-8", is);
            } catch (IOException e) {
                // Jika file tidak ketemu di assets APK
                Log.e("TamodaAssetBridge", "File GAK ADA di APK: " + fileName);
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
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        return "image/png";
    }
}
