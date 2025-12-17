package com.app.whatsinside2.Model

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.app.whatsinside2.ScannerViewModel
import com.app.whatsinside2.Screen
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    navController: NavController,
    // Hier wird das ViewModel geholt
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current

    // Beobachtet den Zustand, ob gerade gescannt wird
    val isScanning by viewModel.isScanning.collectAsState()

    // Sobald diese Activity aufgerufen wird, wird automatisch die Kamera gestartet
    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    // Prüft, ob die Kamerarechte erteilt wurden
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeDetected = { barcode ->
                        // Prüft im ViewModel, ob ein Barcode gefunden wurde
                        if (isScanning) {
                            viewModel.onBarcodeFound() // Ändert den Zustand beim Fund eines Barcodes

                            Log.d("ScannerScreen", "Barcode gefunden: $barcode")
                            navController.navigate(Screen.Details.createRoute(barcode)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                )

                ScannerOverlay(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            navController.navigate(Screen.Details.createRoute("manual_entry"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Ohne Scan hinzufügen"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { navController.popBackStack() }) {
                        Text(
                            text = "Zurück zum Vorratsschrank"
                        )
                    }
                }
            }
        } else {
            /**
             * Folgender Column-Block weißt den User dazu an, der App Rechte auf die Kamera
             * zu erteilen, um Produkte scannen zu können.
             */
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Kamera wird benötigt"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(
                        text = "Kamera freigeben"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Abbrechen")
                }
            }
        }
    }
}


// Hilfsfunktionen für die UI-Elemente

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier){
    Canvas(modifier = modifier) {
        val scanWidth = 300.dp.toPx()
        val scanHeight = 160.dp.toPx()
        val left = (size.width - scanWidth) / 2
        val top = (size.height - scanHeight) / 2
        val right = left + scanWidth
        val bottom = top + scanHeight

        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(Rect(left, top, right, bottom))
            fillType = PathFillType.EvenOdd
        }

        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.6f)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanWidth, scanHeight),
            style = Stroke(width = 3.dp.toPx()
            )
        )
        drawLine(
            color = Color.Red.copy(alpha = 0.8f),
            start = Offset(left + 20f, top + scanHeight / 2),
            end = Offset(right - 20f, top + scanHeight / 2),
            strokeWidth = 2.dp.toPx()
        )
    }
}

// Der Codebereich für das Aufrufen der Kamera
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    processImageProxy(imageProxy, onBarcodeDetected)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Kamera konnte nicht gestartet werden", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { }
    )
}

// Dieser Codeblock prüft, ob ein Barcode mit bestimmten Standards auf dem Kamerabild vorhanden ist
@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Barcode-Standards
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { code -> onBarcodeDetected(code) }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}