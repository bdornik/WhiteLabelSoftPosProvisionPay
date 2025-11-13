package com.payten.whitelabel.ui.screens

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.viewmodel.TermsConditionsViewModel
import java.io.File

/**
 * PDF Viewer screen for displaying Terms & Conditions.
 *
 * Uses AndroidView to integrate the PDF library with Compose.
 *
 * @param pdfUrl URL of the PDF to download and display.
 * @param onNavigateBack Callback when back button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TermsConditionsViewModel = hiltViewModel()
) {
    val pdfFile by viewModel.pdfFileLiveData.observeAsState()
    val isLoading = pdfFile == null

    // Download PDF on first composition
    LaunchedEffect(pdfUrl) {
        viewModel.downloadPdf(pdfUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Uslovi i odredbe",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Nazad"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Učitavanje dokumenta...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                pdfFile != null -> {
                    PdfViewerComponent(file = pdfFile!!)
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Greška pri učitavanju dokumenta",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * PDF viewer component using AndroidView.
 *
 * Integrates the PDF library with Compose.
 */
@Composable
private fun PdfViewerComponent(file: File) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PDFView(context, null).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { pdfView ->
            pdfView.fromFile(file)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(false)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(0)
                .load()
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PdfViewerScreenPreview() {
    AppTheme {
        PdfViewerScreen(
            pdfUrl = "https://www.otpbanka.si/spposflik"
        )
    }
}