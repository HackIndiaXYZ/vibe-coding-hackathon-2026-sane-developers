package com.example.glitchartstudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions

val MatrixGreen = Color(0xFF00FF41)
val DefaultPalette = listOf(Color.Black, Color.Yellow, Color.Red, Color.Cyan, Color.White, MatrixGreen, Color.Magenta, Color.Blue, Color(0xFFFF5722))

data class CustomBar(
    val id: Long = System.currentTimeMillis(), var color: Color = Color.Yellow,
    var xPos: Float = 0.1f, var yPos: Float = 0.4f, var width: Float = 0.8f, var height: Float = 0.05f,
    var isImage: Boolean = false, var uri: Uri? = null, var imageBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    var isText: Boolean = false, var text: String = "", var isHex: Boolean = false, var hexString: List<String> = emptyList(),
    var aspectRatio: Float = 1f
)

data class GlitchSmear(val x: Float, val y: Float, val width: Float, val height: Float, val color: Color)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) { Box(modifier = Modifier.fillMaxSize()) { Image(painter = painterResource(id = R.drawable.matrix_bg), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f); GlitchLabApp() } } } }
    }
}

enum class GlitchPreset(val title: String) {
    MAGENTA_WASH("Magenta Wash"), CYBER_VIOLET("Cyber Violet"), CINEMATIC_MONO("High-Contrast B&W"), CHROME_CORRUPT("Chrome Corruption"), NEON_MATRIX("Neon Matrix"), TOXIC_WASTE("Toxic Waste"), BLOOD_MOON("Blood Moon"), DEEP_OCEAN("Deep Ocean"), GHOST_MACHINE("Ghost Machine"), GOLDEN_ERA("Golden Era"), TERMINAL_AMBER("Terminal Amber"), VAMPIRE_NIGHT("Vampire Night"), ALIEN_FLORA("Alien Flora"), CORRUPTED_VHS("Corrupted VHS"), ELECTRIC_INDIGO("Electric Indigo"), OVEREXPOSED("Overexposed"), UNDERGROUND_CLUB("Underground Club"), RADIOACTIVE("Radioactive"), COLD_STEEL("Cold Steel"), VOID_BLACK("Void Black")
}

@Composable
fun GlitchyTitle() {
    Box(contentAlignment = Alignment.Center) {
        Text("GlitchLab", color = Color.Cyan, fontSize = 56.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(x = (-4).dp, y = 2.dp))
        Text("GlitchLab", color = Color.Red, fontSize = 56.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(x = 4.dp, y = (-2).dp))
        Text("GlitchLab", color = MatrixGreen, fontSize = 56.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun GlitchLabApp() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isComparing by remember { mutableStateOf(false) }
    var isProMode by remember { mutableStateOf(false) }

    var glitchTarget by remember { mutableStateOf("ALL") }
    var subjectMask by remember { mutableStateOf<Bitmap?>(null) }
    var subjectAlphaMask by remember { mutableStateOf<Bitmap?>(null) }
    var sinCityMode by remember { mutableStateOf(false) }

    // IMAGE ASPECT TRACKER STATE
    var imageAspectRatio by remember { mutableStateOf<Float?>(null) }
    var neonGlowMode by remember { mutableStateOf(false) }
    var neonGlowColor by remember { mutableStateOf(MatrixGreen) }

    var activePreset by remember { mutableStateOf(GlitchPreset.TOXIC_WASTE) }
    var stdIntensity by remember { mutableFloatStateOf(1f) }
    var stdOverdrive by remember { mutableFloatStateOf(1f) }
    var proIntensity by remember { mutableFloatStateOf(1f) }
    var proOverdrive by remember { mutableFloatStateOf(1f) }
    var proTrailCount by remember { mutableFloatStateOf(0f) }
    var proTrailSpacing by remember { mutableFloatStateOf(1f) }
    var brushIntensity by remember { mutableFloatStateOf(0.5f) }
    var isEraserActive by remember { mutableStateOf(false) }
    var brushMode by remember { mutableStateOf("SCATTER") }

    var globalTint by remember { mutableStateOf(Color.Unspecified) }
    var leftGlitchColor by remember { mutableStateOf(Color.Cyan) }
    var rightGlitchColor by remember { mutableStateOf(Color.Red) }

    var customBars by remember { mutableStateOf(listOf<CustomBar>()) }
    var smearBlocks by remember { mutableStateOf(listOf<GlitchSmear>()) }

    var selectedBarId by remember { mutableStateOf<Long?>(null) }
    var activeInteraction by remember { mutableStateOf("NONE") }
    var activeProTool by remember { mutableStateOf("NONE") }

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var dialogType by remember { mutableStateOf("TEXT") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        imageUri = uri
        subjectMask = null
        subjectAlphaMask = null
        imageAspectRatio = null
        glitchTarget = "ALL"
        neonGlowMode = false
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; d.isMutableRequired = true } } else { MediaStore.Images.Media.getBitmap(context.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true) }

                    withContext(Dispatchers.Main) {
                        imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    }

                    val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
                    segmenter.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { result ->
                        val fg = result.foregroundBitmap
                        subjectMask = fg
                        if (fg != null) { subjectAlphaMask = fg.extractAlpha() }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBuffer = if (navPadding > 0.dp) navPadding + 8.dp else 32.dp

    val layerPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }.asImageBitmap() } else { MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap() }
                    withContext(Dispatchers.Main) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        customBars = customBars + CustomBar(isImage = true, uri = uri, imageBitmap = bitmap, aspectRatio = ratio, width = (0.2f * ratio).coerceAtMost(0.8f), height = 0.2f)
                        selectedBarId = customBars.last().id; activeProTool = "NONE"
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            confirmButton = {
                Button(onClick = {
                    if (textInput.isNotBlank()) {
                        if (dialogType == "HEX") {
                            val binaryStr = textInput.toByteArray().joinToString("") { String.format("%8s", Integer.toBinaryString(it.toInt() and 0xFF)).replace(' ', '0') }
                            val binaryLines = binaryStr.chunked(16)
                            val paint = android.graphics.Paint().apply { textSize = 150f; color = android.graphics.Color.WHITE; isAntiAlias = true; typeface = android.graphics.Typeface.MONOSPACE; textAlign = android.graphics.Paint.Align.LEFT }
                            val fm = paint.fontMetrics
                            val lineHeight = fm.descent - fm.ascent
                            val w = (paint.measureText(binaryLines.maxByOrNull { it.length } ?: "0") + 20f).toInt()
                            val h = (lineHeight * binaryLines.size + 20f).toInt()
                            if (w > 0 && h > 0) {
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                var currentY = -fm.ascent + 10f
                                binaryLines.forEach { line -> canvas.drawText(line, 10f, currentY, paint); currentY += lineHeight }
                                val ratio = w.toFloat() / h.toFloat()
                                val startH = 0.25f
                                customBars = customBars + CustomBar(isHex = true, text = "Binary Block", isImage = true, imageBitmap = bmp.asImageBitmap(), aspectRatio = ratio, width = (startH * ratio).coerceAtMost(0.9f), height = startH, color = MatrixGreen)
                                selectedBarId = customBars.last().id
                            }
                        } else {
                            val paint = android.graphics.Paint().apply { textSize = 300f; color = android.graphics.Color.WHITE; isAntiAlias = true; textAlign = android.graphics.Paint.Align.LEFT }
                            val baseline = -paint.fontMetrics.ascent
                            val w = (paint.measureText(textInput) + 20f).toInt()
                            val h = (baseline + paint.fontMetrics.descent + 20f).toInt()
                            if (w > 0 && h > 0) {
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                android.graphics.Canvas(bmp).drawText(textInput, 10f, baseline + 10f, paint)
                                val ratio = w.toFloat() / h.toFloat()
                                val startH = 0.15f
                                customBars = customBars + CustomBar(isText = true, text = textInput, isImage = true, imageBitmap = bmp.asImageBitmap(), aspectRatio = ratio, width = (startH * ratio).coerceAtMost(0.9f), height = startH, color = Color.Transparent)
                                selectedBarId = customBars.last().id
                            }
                        }
                        activeProTool = "NONE"
                    }
                    showTextInputDialog = false; textInput = ""
                }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = Color.Black)) { Text("Add") }
            },
            title = { Text(if(dialogType=="HEX") "Inject Binary Message" else "Add Text Line", color = Color.White) }, text = { TextField(value = textInput, onValueChange = { textInput = it }) }, containerColor = Color.DarkGray
        )
    }

    var startBarState by remember { mutableStateOf<CustomBar?>(null) }
    var cumulativeDragOffset by remember { mutableStateOf(Offset.Zero) }
    fun randFloat(min: Float, max: Float): Float = min + (Math.random().toFloat() * (max - min))
    fun paintBrushSmear(pxX: Float, pxY: Float) {
        if (isEraserActive) { smearBlocks = smearBlocks.filterNot { Math.hypot((it.x + (it.width/2) - pxX).toDouble(), (it.y + (it.height/2) - pxY).toDouble()) < 0.05f + (brushIntensity * 0.05f) } }
        else {
            val count = if(brushMode == "DATAMOSH") (1 + (3 * brushIntensity)).toInt() else (2 + (10 * brushIntensity)).toInt()
            val spread = 0.02f + (0.08f * brushIntensity)
            val newBlocks = (1..count).map {
                if (brushMode == "DATAMOSH") GlitchSmear(x = pxX, y = pxY + randFloat(-0.03f, 0.03f), width = randFloat(0.1f, 0.5f * brushIntensity), height = randFloat(0.002f, 0.01f), color = listOf(leftGlitchColor, rightGlitchColor, Color.DarkGray, Color.White).random().copy(alpha = 0.6f))
                else GlitchSmear(x = pxX + randFloat(-spread, spread), y = pxY + randFloat(-spread / 2, spread / 2), width = randFloat(0.01f, 0.1f * brushIntensity.coerceAtLeast(0.5f)), height = randFloat(0.002f, 0.02f), color = listOf(leftGlitchColor, rightGlitchColor, Color.Black, Color.White, MatrixGreen).random().copy(alpha = 0.8f))
            }
            smearBlocks = smearBlocks + newBlocks
        }
    }

    if (imageUri == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { GlitchyTitle(); Spacer(modifier = Modifier.height(32.dp)); Button(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = Color.Black), modifier = Modifier.padding(16.dp).height(50.dp).width(200.dp)) { Text("SELECT IMAGE", fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.DarkGray), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Chg", fontSize = 12.sp) }
                if (isProMode) { Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.DarkGray).pointerInput(Unit) { detectTapGestures(onPress = { isComparing = true; tryAwaitRelease(); isComparing = false }) }.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) { Text("Orig", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) } }
                Button(onClick = { isProMode = !isProMode; selectedBarId = null; activeProTool = "NONE" }, colors = ButtonDefaults.buttonColors(containerColor = if(isProMode) Color.Red else MatrixGreen, contentColor = Color.Black), contentPadding = PaddingValues(horizontal = 12.dp)) { Text(if(isProMode) "EXIT PRO" else "PRO STUDIO", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                Button(onClick = { isSaving = true; selectedBarId = null; coroutineScope.launch { val success = saveGlitchLabToGallery(context, imageUri!!, isProMode, activePreset, stdIntensity, stdOverdrive, proIntensity, proOverdrive, proTrailCount, proTrailSpacing, glitchTarget, sinCityMode, neonGlowMode, neonGlowColor, subjectMask, subjectAlphaMask, customBars, smearBlocks, leftGlitchColor, rightGlitchColor, globalTint); isSaving = false; Toast.makeText(context, if(success) "Saved!" else "Save Failed.", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = MatrixGreen), enabled = !isSaving, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(if (isSaving) "..." else "Save", fontSize = 12.sp) }
            }

            // DYNAMIC ASPECT HOOK INJECTED HERE (ELIMINATES COORD DRIFT)
            Box(modifier = Modifier.weight(1f).padding(16.dp).fillMaxWidth()
                .then(if (imageAspectRatio != null) Modifier.aspectRatio(imageAspectRatio!!) else Modifier)
                .pointerInput(isProMode, customBars, activeProTool, isEraserActive, brushIntensity, brushMode) {
                    detectTapGestures(
                        onPress = { if (!isProMode) { isComparing = true; tryAwaitRelease(); isComparing = false } },
                        onTap = { tapOffset ->
                            if (isProMode) {
                                if (activeProTool == "BRUSH") paintBrushSmear(tapOffset.x / size.width, tapOffset.y / size.height)
                                else { selectedBarId = customBars.findLast { tapOffset.x in (size.width * it.xPos)..(size.width * (it.xPos + it.width)) && tapOffset.y in (size.height * it.yPos)..(size.height * (it.yPos + it.height)) }?.id; if (selectedBarId != null) activeProTool = "NONE" }
                            }
                        }
                    )
                }
                .pointerInput(isProMode, selectedBarId, activeProTool, isEraserActive, brushIntensity, brushMode) {
                    if (isProMode) {
                        if (activeProTool == "BRUSH") detectDragGestures { change, _ -> change.consume(); paintBrushSmear(change.position.x / size.width, change.position.y / size.height) }
                        else if (selectedBarId != null) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val bar = customBars.find { it.id == selectedBarId } ?: return@detectDragGestures
                                    startBarState = bar; cumulativeDragOffset = Offset.Zero; val pxX = size.width * bar.xPos; val pxY = size.height * bar.yPos; val pxW = size.width * bar.width; val pxH = size.height * bar.height; val hitRadius = 80f
                                    activeInteraction = when {
                                        startOffset.x in (pxX + pxW - hitRadius)..(pxX + pxW + hitRadius) && startOffset.y in (pxY + pxH - hitRadius)..(pxY + pxH + hitRadius) -> "RESIZE_BR"
                                        startOffset.x in (pxX - hitRadius)..(pxX + hitRadius) && startOffset.y in (pxY + pxH - hitRadius)..(pxY + pxH + hitRadius) -> "RESIZE_BL"
                                        startOffset.x in (pxX + pxW - hitRadius)..(pxX + pxW + hitRadius) && startOffset.y in (pxY - hitRadius)..(pxY + hitRadius) -> "RESIZE_TR"
                                        startOffset.x in (pxX - hitRadius)..(pxX + hitRadius) && startOffset.y in (pxY - hitRadius)..(pxY + hitRadius) -> "RESIZE_TL"
                                        startOffset.x in pxX..(pxX + pxW) && startOffset.y in pxY..(pxY + pxH) -> "MOVE"
                                        else -> "NONE"
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume(); cumulativeDragOffset += dragAmount; val totalDx = cumulativeDragOffset.x / size.width; val totalDy = cumulativeDragOffset.y / size.height
                                    customBars = customBars.map { bar ->
                                        if (bar.id == selectedBarId && startBarState != null) {
                                            val startBar = startBarState!!; val minW = 0.01f; val minH = 0.002f; val isUniform = startBar.isImage
                                            when (activeInteraction) {
                                                "MOVE" -> startBar.copy(xPos = startBar.xPos + totalDx, yPos = startBar.yPos + totalDy)
                                                "RESIZE_BR" -> { if(isUniform){ val newH = (startBar.height + totalDy).coerceAtLeast(minH); startBar.copy(width = newH * startBar.aspectRatio, height = newH) } else startBar.copy(width = (startBar.width + totalDx).coerceAtLeast(minW), height = (startBar.height + totalDy).coerceAtLeast(minH)) }
                                                "RESIZE_BL" -> { if(isUniform){ val newH = (startBar.height + totalDy).coerceAtLeast(minH); val newW = newH * startBar.aspectRatio; startBar.copy(xPos = startBar.xPos - (newW - startBar.width), width = newW, height = newH) } else startBar.copy(xPos = startBar.xPos + totalDx, width = (startBar.width - totalDx).coerceAtLeast(minW), height = (startBar.height + totalDy).coerceAtLeast(minH)) }
                                                "RESIZE_TR" -> { if(isUniform){ val newH = (startBar.height - totalDy).coerceAtLeast(minH); startBar.copy(yPos = startBar.yPos + totalDy, width = newH * startBar.aspectRatio, height = newH) } else startBar.copy(yPos = startBar.yPos + totalDy, width = (startBar.width + totalDx).coerceAtLeast(minW), height = (startBar.height - totalDy).coerceAtLeast(minH)) }
                                                "RESIZE_TL" -> { if(isUniform){ val newH = (startBar.height - totalDy).coerceAtLeast(minH); val newW = newH * startBar.aspectRatio; startBar.copy(xPos = startBar.xPos - (newW - startBar.width), yPos = startBar.yPos + totalDy, width = newW, height = newH) } else startBar.copy(xPos = startBar.xPos + totalDx, yPos = startBar.yPos + totalDy, width = (startBar.width - totalDx).coerceAtLeast(minW), height = (startBar.height - totalDy).coerceAtLeast(minH)) }
                                                else -> bar
                                            }
                                        } else bar
                                    }
                                },
                                onDragEnd = { activeInteraction = "NONE"; startBarState = null }
                            )
                        }
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                if (isComparing) {
                    AsyncImage(model = imageUri, contentDescription = "Original", modifier = Modifier.fillMaxSize())
                } else {
                    val currentIntensity = if (isProMode) proIntensity else stdIntensity
                    val currentOverdrive = if (isProMode) proOverdrive else stdOverdrive
                    val rotation = if (!isProMode && activePreset == GlitchPreset.MAGENTA_WASH) 1.5f * currentIntensity else -1.0f * currentIntensity
                    val baseShiftDp = 6.dp * currentIntensity
                    val bgModifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.05f; scaleY = 0.95f; rotationZ = rotation }

                    val isSubjectGlitch = isProMode && glitchTarget == "SUBJECT" && subjectMask != null
                    val isWorldGlitch = isProMode && glitchTarget == "WORLD" && subjectMask != null
                    val glitchModel = if (isSubjectGlitch) subjectMask else imageUri

                    if (isSubjectGlitch) {
                        AsyncImage(model = imageUri, contentDescription = "Clean Background", modifier = Modifier.fillMaxSize())
                    }

                    val filterMatrix = if (sinCityMode) getSinCityMatrix(currentOverdrive) else if (isProMode) getDynamicMatrix(currentOverdrive, globalTint) else getPresetMatrix(activePreset, currentOverdrive)
                    AsyncImage(model = glitchModel, contentDescription = null, colorFilter = ColorFilter.colorMatrix(filterMatrix), modifier = bgModifier)

                    if (isProMode && proTrailCount > 0) {
                        val trailBaseShiftDp = 12.dp; val trailColorInt = 1.5f
                        for (i in 1..proTrailCount.toInt()) {
                            val trailAlpha = 0.9f - (i * 0.12f)
                            if (trailAlpha > 0) {
                                val trailShift = (trailBaseShiftDp * i) * proTrailSpacing
                                AsyncImage(model = glitchModel, contentDescription = null, alpha = trailAlpha, colorFilter = ColorFilter.colorMatrix(getColorGlitchMatrix(leftGlitchColor, trailColorInt)), modifier = Modifier.fillMaxSize().offset(x = -trailShift).graphicsLayer { scaleX = 1.05f; scaleY = 0.95f; rotationZ = rotation })
                                AsyncImage(model = glitchModel, contentDescription = null, alpha = trailAlpha, colorFilter = ColorFilter.colorMatrix(getColorGlitchMatrix(rightGlitchColor, trailColorInt)), modifier = Modifier.fillMaxSize().offset(x = trailShift).graphicsLayer { scaleX = 1.05f; scaleY = 0.95f; rotationZ = rotation })
                            }
                        }
                    }

                    AsyncImage(model = glitchModel, contentDescription = null, colorFilter = ColorFilter.colorMatrix(if (isProMode) getColorGlitchMatrix(leftGlitchColor, currentIntensity) else getCyanMatrix(currentIntensity)), modifier = Modifier.fillMaxSize().offset(x = -baseShiftDp).graphicsLayer { scaleX = 1.05f; scaleY = 0.95f; rotationZ = rotation })
                    AsyncImage(model = glitchModel, contentDescription = null, colorFilter = ColorFilter.colorMatrix(if (isProMode) getColorGlitchMatrix(rightGlitchColor, currentIntensity) else getRedMatrix(currentIntensity)), modifier = Modifier.fillMaxSize().offset(x = baseShiftDp).graphicsLayer { scaleX = 1.05f; scaleY = 0.95f; rotationZ = rotation })

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (!isProMode) { drawGlitchesForPreset(activePreset, size.width, size.height, stdIntensity) }
                        else { smearBlocks.forEach { smear -> drawRect(color = smear.color, topLeft = Offset(size.width * smear.x, size.height * smear.y), size = Size(size.width * smear.width, size.height * smear.height)) } }
                    }

                    // NEON VECTOR STENCIL OUTLINE ENGINE (LOCKED TO IMAGE VIEW bounds)
                    if (isProMode && neonGlowMode && subjectAlphaMask != null) {
                        Canvas(modifier = bgModifier) {
                            drawIntoCanvas { canvas ->
                                val native = canvas.nativeCanvas
                                val p = android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb(255, (neonGlowColor.red * 255).toInt(), (neonGlowColor.green * 255).toInt(), (neonGlowColor.blue * 255).toInt())
                                    isAntiAlias = true
                                }
                                val srcRect = android.graphics.Rect(0, 0, subjectAlphaMask!!.width, subjectAlphaMask!!.height)
                                val layerId = native.saveLayer(0f, 0f, size.width, size.height, null)

                                val thick = (size.width * 0.005f).toInt().coerceAtLeast(3)
                                native.drawBitmap(subjectAlphaMask!!, srcRect, android.graphics.Rect(-thick, 0, size.width.toInt() - thick, size.height.toInt()), p)
                                native.drawBitmap(subjectAlphaMask!!, srcRect, android.graphics.Rect(thick, 0, size.width.toInt() + thick, size.height.toInt()), p)
                                native.drawBitmap(subjectAlphaMask!!, srcRect, android.graphics.Rect(0, -thick, size.width.toInt(), size.height.toInt() - thick), p)
                                native.drawBitmap(subjectAlphaMask!!, srcRect, android.graphics.Rect(0, thick, size.width.toInt(), size.height.toInt() + thick), p)

                                p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
                                native.drawBitmap(subjectAlphaMask!!, srcRect, android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt()), p)

                                native.restoreToCount(layerId)
                            }
                        }
                    }

                    if (isWorldGlitch) {
                        Image(bitmap = subjectMask!!.asImageBitmap(), contentDescription = "Protected Subject", modifier = bgModifier)
                    }

                    if (isProMode) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            customBars.forEach { bar ->
                                val pxX = size.width * bar.xPos; val pxY = size.height * bar.yPos; val pxW = size.width * bar.width; val pxH = size.height * bar.height
                                if (bar.isImage && bar.imageBitmap != null) {
                                    val tint = if (bar.isHex) ColorFilter.tint(bar.color) else null
                                    drawImage(image = bar.imageBitmap!!, dstOffset = IntOffset(pxX.toInt(), pxY.toInt()), dstSize = IntSize(pxW.toInt(), pxH.toInt()), colorFilter = tint)
                                } else { drawRect(color = bar.color, topLeft = Offset(pxX, pxY), size = Size(pxW, pxH), alpha = 0.85f) }

                                if (bar.id == selectedBarId) {
                                    drawRect(color = Color.White, topLeft = Offset(pxX, pxY), size = Size(pxW, pxH), style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)))
                                    val dotRad = 16f
                                    drawCircle(Color.White, radius = dotRad, center = Offset(pxX, pxY)); drawCircle(Color.White, radius = dotRad, center = Offset(pxX + pxW, pxY)); drawCircle(Color.White, radius = dotRad, center = Offset(pxX, pxY + pxH)); drawCircle(Color.White, radius = dotRad, center = Offset(pxX + pxW, pxY + pxH))
                                }
                            }
                        }
                    }
                }
            }
            if (!isProMode) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 0.dp)) {
                    Text(text = "Intensity: ${(stdIntensity * 100).toInt()}%", color = MatrixGreen, style = MaterialTheme.typography.labelMedium)
                    Slider(value = stdIntensity, onValueChange = { stdIntensity = it }, valueRange = 0f..2f, colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen), modifier = Modifier.height(24.dp))
                    Text(text = "Overdrive: ${(stdOverdrive * 100).toInt()}%", color = MatrixGreen, style = MaterialTheme.typography.labelMedium)
                    Slider(value = stdOverdrive, onValueChange = { stdOverdrive = it }, valueRange = 0f..4f, colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen), modifier = Modifier.height(24.dp))
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = bottomBuffer, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { GlitchPreset.values().forEach { preset -> FilterChip(selected = activePreset == preset, onClick = { activePreset = preset }, label = { Text(preset.title) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black, labelColor = Color.LightGray)) } }
            } else {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(bottom = bottomBuffer)) {
                    if (selectedBarId != null) {
                        val bar = customBars.find { it.id == selectedBarId }
                        if (bar != null) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(if(bar.isHex) "Binary Hex Selected" else if(bar.isText) "Text Layer Selected" else if(bar.isImage) "Sticker Selected" else "Neon Bar Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    OutlinedButton(onClick = { customBars = customBars.filter { it.id != selectedBarId }; selectedBarId = null }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red), modifier = Modifier.height(36.dp)) { Text("DELETE LAYER", fontSize = 12.sp) }
                                }
                                if (bar.isHex || (!bar.isImage && !bar.isText)) { ColorPaletteSelector(selectedColor = bar.color, onColorSelected = { newC -> customBars = customBars.map { if (it.id == bar.id) it.copy(color = newC) else it } }, includeClear = false) }
                            }
                        }
                    } else {
                        when (activeProTool) {
                            "TUNE" -> {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                                    Text("GLITCH TARGET (AI)", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = glitchTarget == "ALL", onClick = { glitchTarget = "ALL" }, label = { Text("Off (Full Image)") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                                        FilterChip(selected = glitchTarget == "WORLD", onClick = { glitchTarget = "WORLD" }, enabled = subjectMask != null, label = { Text("World") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                                        FilterChip(selected = glitchTarget == "SUBJECT", onClick = { glitchTarget = "SUBJECT" }, enabled = subjectMask != null, label = { Text("Subject") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
                                    Text("GLOBAL INTENSITY", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp); Slider(value = proIntensity, onValueChange = { proIntensity = it }, valueRange = 0f..3f, colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen), modifier = Modifier.height(24.dp))
                                    Text("GLOBAL OVERDRIVE", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top=8.dp)); Slider(value = proOverdrive, onValueChange = { proOverdrive = it }, valueRange = 0f..5f, colors = SliderDefaults.colors(thumbColor = MatrixGreen, activeTrackColor = MatrixGreen), modifier = Modifier.height(24.dp))
                                }
                            }
                            "TRAIL" -> {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                                    Text("MOTION TRAIL CLONES", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp); Slider(value = proTrailCount, onValueChange = { proTrailCount = it }, valueRange = 0f..5f, steps = 4, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White), modifier = Modifier.height(24.dp))
                                    Text("MOTION TRAIL SPACING", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top=8.dp)); Slider(value = proTrailSpacing, onValueChange = { proTrailSpacing = it }, valueRange = 0.5f..3f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White), modifier = Modifier.height(24.dp))
                                }
                            }
                            "COLORS" -> {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("SIN CITY PROTOCOL (B&W)", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Switch(checked = sinCityMode, onCheckedChange = { sinCityMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen, checkedTrackColor = Color.DarkGray))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("NEON EDGE OUTLINE", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Switch(checked = neonGlowMode, onCheckedChange = { neonGlowMode = it }, enabled = subjectAlphaMask != null, colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen, checkedTrackColor = Color.DarkGray))
                                    }
                                    if (neonGlowMode) {
                                        ColorPaletteSelector(selectedColor = neonGlowColor, onColorSelected = { neonGlowColor = it }, includeClear = false)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
                                    Text("BASE TINT", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp); ColorPaletteSelector(selectedColor = globalTint, onColorSelected = { globalTint = it }, includeClear = true)
                                    Row(modifier = Modifier.fillMaxWidth().padding(top=8.dp)) {
                                        Column(modifier = Modifier.weight(1f)) { Text("Left Ghost", color = Color.White, fontSize = 12.sp); ColorPaletteSelector(leftGlitchColor, { leftGlitchColor = it }, false) }
                                        Column(modifier = Modifier.weight(1f)) { Text("Right Ghost", color = Color.White, fontSize = 12.sp); ColorPaletteSelector(rightGlitchColor, { rightGlitchColor = it }, false) }
                                    }
                                }
                            }
                            "BRUSH" -> {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("CORRUPT BRUSH", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Datamosh", color = if (brushMode == "DATAMOSH") MatrixGreen else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)); Switch(checked = brushMode == "DATAMOSH", onCheckedChange = { brushMode = if(it) "DATAMOSH" else "SCATTER" }, colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen, checkedTrackColor = Color.DarkGray)) }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Eraser Mode", color = if (isEraserActive) Color.Red else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)); Switch(checked = isEraserActive, onCheckedChange = { isEraserActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.DarkGray))
                                    }
                                    if (!isEraserActive) { Text("BRUSH INTENSITY", color = MatrixGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(top=8.dp)); Slider(value = brushIntensity, onValueChange = { brushIntensity = it }, valueRange = 0.1f..1.5f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White), modifier = Modifier.height(24.dp)) }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("(Drag over image to paint)", color = Color.LightGray, fontSize = 10.sp); OutlinedButton(onClick = { smearBlocks = emptyList() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("CLEAR BRUSH", fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.DarkGray)

                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = activeProTool == "TUNE" && selectedBarId == null, onClick = { activeProTool = if(activeProTool == "TUNE") "NONE" else "TUNE"; selectedBarId = null }, label = { Text("Tune") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                        FilterChip(selected = activeProTool == "TRAIL" && selectedBarId == null, onClick = { activeProTool = if(activeProTool == "TRAIL") "NONE" else "TRAIL"; selectedBarId = null }, label = { Text("Trail") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                        FilterChip(selected = activeProTool == "COLORS" && selectedBarId == null, onClick = { activeProTool = if(activeProTool == "COLORS") "NONE" else "COLORS"; selectedBarId = null }, label = { Text("Colors") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))
                        FilterChip(selected = activeProTool == "BRUSH" && selectedBarId == null, onClick = { activeProTool = if(activeProTool == "BRUSH") "NONE" else "BRUSH"; selectedBarId = null }, label = { Text("Corrupt") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MatrixGreen, selectedLabelColor = Color.Black))

                        Button(onClick = { dialogType = "HEX"; showTextInputDialog = true; activeProTool = "NONE" }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = Color.Black)) { Text("+ Hex") }
                        Button(onClick = { val n = CustomBar(); customBars = customBars + n; selectedBarId = n.id; activeProTool = "NONE" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) { Text("+ Bar") }
                        Button(onClick = { layerPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); activeProTool = "NONE" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) { Text("+ Sticker") }
                        Button(onClick = { dialogType = "TEXT"; showTextInputDialog = true; activeProTool = "NONE" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) { Text("+ Text") }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPaletteSelector(selectedColor: Color, onColorSelected: (Color) -> Unit, includeClear: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeClear) Box(modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).clickable { onColorSelected(Color.Unspecified) }, contentAlignment = Alignment.Center) { Text("X", color = Color.White, fontSize = 10.sp) }
        DefaultPalette.forEach { color -> Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color).border(2.dp, if (selectedColor == color) Color.White else Color.Transparent, CircleShape).clickable { onColorSelected(color) }) }
    }
}

suspend fun saveGlitchLabToGallery(context: Context, uri: Uri, isPro: Boolean, preset: GlitchPreset, sInt: Float, sOvr: Float, pInt: Float, pOvr: Float, trailC: Float, trailS: Float, target: String, sinCity: Boolean, neonGlow: Boolean, neonColor: Color, subMask: Bitmap?, subAlpha: Bitmap?, bars: List<CustomBar>, smears: List<GlitchSmear>, lCol: Color, rCol: Color, gTint: Color): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; d.isMutableRequired = true } } else { MediaStore.Images.Media.getBitmap(resolver, uri).copy(Bitmap.Config.ARGB_8888, true) }
            val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
            val androidCanvas = android.graphics.Canvas(resultBitmap)
            androidCanvas.drawColor(android.graphics.Color.BLACK)

            val intensity = if(isPro) pInt else sInt
            val overdrive = if(isPro) pOvr else sOvr
            val baseShiftPx = originalBitmap.width * 0.012f * intensity

            val isSubjectGlitch = isPro && target == "SUBJECT" && subMask != null
            val isWorldGlitch = isPro && target == "WORLD" && subMask != null
            val glitchSource = if (isSubjectGlitch) subMask else originalBitmap
            if (isSubjectGlitch) { androidCanvas.drawBitmap(originalBitmap, 0f, 0f, null) }

            val basePaint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(if (sinCity) getSinCityMatrix(overdrive).values else if(isPro) getDynamicMatrix(overdrive, gTint).values else getPresetMatrix(preset, overdrive).values) }
            androidCanvas.drawBitmap(glitchSource!!, 0f, 0f, basePaint)

            if (isPro && trailC > 0) {
                val trailColorInt = 1.5f
                for (i in 1..trailC.toInt()) {
                    val trailAlpha = 0.9f - (i * 0.12f)
                    if (trailAlpha > 0) {
                        val trailShift = (baseShiftPx * i) * trailS
                        val tLeftPaint = android.graphics.Paint().apply { alpha = (trailAlpha * 255).toInt(); colorFilter = android.graphics.ColorMatrixColorFilter(getColorGlitchMatrix(lCol, trailColorInt).values) }
                        androidCanvas.drawBitmap(glitchSource, -trailShift, 0f, tLeftPaint)
                        val tRightPaint = android.graphics.Paint().apply { alpha = (trailAlpha * 255).toInt(); colorFilter = android.graphics.ColorMatrixColorFilter(getColorGlitchMatrix(rCol, trailColorInt).values) }
                        androidCanvas.drawBitmap(glitchSource, trailShift, 0f, tRightPaint)
                    }
                }
            }

            val leftPaint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(if(isPro) getColorGlitchMatrix(lCol, intensity).values else getCyanMatrix(intensity).values) }
            androidCanvas.drawBitmap(glitchSource, -baseShiftPx, 0f, leftPaint)
            val rightPaint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(if(isPro) getColorGlitchMatrix(rCol, intensity).values else getRedMatrix(intensity).values) }
            androidCanvas.drawBitmap(glitchSource, baseShiftPx, 0f, rightPaint)

            if (!isPro) { drawGlitchesOnAndroidCanvas(androidCanvas, preset, originalBitmap.width.toFloat(), originalBitmap.height.toFloat(), intensity) }
            else {
                smears.forEach { smear ->
                    val sPaint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL; color = android.graphics.Color.argb((0.8f * 255).toInt(), (smear.color.red * 255).toInt(), (smear.color.green * 255).toInt(), (smear.color.blue * 255).toInt()) }
                    androidCanvas.drawRect(originalBitmap.width * smear.x, originalBitmap.height * smear.y, (originalBitmap.width * smear.x) + (originalBitmap.width * smear.width), (originalBitmap.height * smear.y) + (originalBitmap.height * smear.height), sPaint)
                }

                if (neonGlow && subAlpha != null) {
                    val p = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(255, (neonColor.red * 255).toInt(), (neonColor.green * 255).toInt(), (neonColor.blue * 255).toInt())
                        isAntiAlias = true
                    }
                    val w = originalBitmap.width
                    val h = originalBitmap.height
                    val srcRect = android.graphics.Rect(0, 0, subAlpha.width, subAlpha.height)
                    val layerId = androidCanvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)

                    val thick = (w * 0.004f).toInt().coerceAtLeast(4)
                    androidCanvas.drawBitmap(subAlpha, srcRect, android.graphics.Rect(-thick, 0, w - thick, h), p)
                    androidCanvas.drawBitmap(subAlpha, srcRect, android.graphics.Rect(thick, 0, w + thick, h), p)
                    androidCanvas.drawBitmap(subAlpha, srcRect, android.graphics.Rect(0, -thick, w, h - thick), p)
                    androidCanvas.drawBitmap(subAlpha, srcRect, android.graphics.Rect(0, thick, w, h + thick), p)

                    p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
                    androidCanvas.drawBitmap(subAlpha, srcRect, android.graphics.Rect(0, 0, w, h), p)
                    androidCanvas.restoreToCount(layerId)
                }

                if (isWorldGlitch) { androidCanvas.drawBitmap(subMask!!, 0f, 0f, null) }

                bars.forEach { bar ->
                    val pxX = originalBitmap.width * bar.xPos; val pxY = originalBitmap.height * bar.yPos; val pxW = originalBitmap.width * bar.width; val pxH = originalBitmap.height * bar.height
                    if (bar.isImage) {
                        try {
                            val overlay = if (bar.uri != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, bar.uri!!)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; d.isMutableRequired = true } } else { MediaStore.Images.Media.getBitmap(resolver, bar.uri!!).copy(Bitmap.Config.ARGB_8888, true) }
                            } else if (bar.imageBitmap != null) { bar.imageBitmap!!.asAndroidBitmap() } else null

                            if (overlay != null) {
                                val scaledOverlay = Bitmap.createScaledBitmap(overlay, pxW.toInt(), pxH.toInt(), true)
                                val paint = if (bar.isHex) { android.graphics.Paint().apply { val c = android.graphics.Color.argb(255, (bar.color.red*255).toInt(), (bar.color.green*255).toInt(), (bar.color.blue*255).toInt()); colorFilter = android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN) } } else null
                                androidCanvas.drawBitmap(scaledOverlay, pxX, pxY, paint)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        val cPaint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL; color = android.graphics.Color.argb((0.85f * 255).toInt(), (bar.color.red * 255).toInt(), (bar.color.green * 255).toInt(), (bar.color.blue * 255).toInt()) }
                        androidCanvas.drawRect(pxX, pxY, pxX + pxW, pxY + pxH, cPaint)
                    }
                }
            }

            val cv = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "GlitchLab_${System.currentTimeMillis()}.jpg"); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.IS_PENDING, 1) }
            val outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            if (outUri != null) { resolver.openOutputStream(outUri)?.use { s -> resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, s) }; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(outUri, cv, null, null) }; return@withContext true }
            return@withContext false
        } catch (e: Exception) { return@withContext false }
    }
}

fun getCyanMatrix(intensity: Float): ColorMatrix { val alpha = (0.35f * intensity).coerceIn(0f, 0.4f); return ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, alpha, 0f)) }
fun getRedMatrix(intensity: Float): ColorMatrix { val alpha = (0.35f * intensity).coerceIn(0f, 0.4f); return ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, alpha, 0f)) }
fun getColorGlitchMatrix(color: Color, intensity: Float): ColorMatrix { val alpha = (0.35f * intensity).coerceIn(0f, 0.5f); return ColorMatrix(floatArrayOf(color.red, 0f, 0f, 0f, 0f, 0f, color.green, 0f, 0f, 0f, 0f, 0f, color.blue, 0f, 0f, 0f, 0f, 0f, alpha, 0f)) }

fun getSinCityMatrix(overdrive: Float): ColorMatrix {
    val b = floatArrayOf(0.33f * overdrive, 0.59f * overdrive, 0.11f * overdrive, 0f, 0f, 0.33f * overdrive, 0.59f * overdrive, 0.11f * overdrive, 0f, 0f, 0.33f * overdrive, 0.59f * overdrive, 0.11f * overdrive, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
    return ColorMatrix(b)
}

fun getDynamicMatrix(overdrive: Float, tint: Color): ColorMatrix {
    val b = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
    for (i in 0..14) { if (i % 5 == 4) b[i] = (b[i] * overdrive) + ((overdrive - 1f) * 25f) else b[i] *= overdrive }
    if (tint != Color.Unspecified) { b[0] *= tint.red; b[6] *= tint.green; b[12] *= tint.blue }
    return ColorMatrix(b)
}

fun getPresetMatrix(preset: GlitchPreset, overdrive: Float): ColorMatrix {
    val b = when (preset) {
        GlitchPreset.MAGENTA_WASH -> floatArrayOf(2.5f, -0.5f, 0.0f, 0.0f, 40f, 0.0f, 2.0f, -0.5f, 0.0f, -20f, -0.5f, 0.0f, 2.5f, 0.0f, 60f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.CYBER_VIOLET -> floatArrayOf(1.2f, 0.0f, 0.5f, 0.0f, 10f, 0.0f, 0.8f, 0.0f, 0.0f, -10f, 0.5f, 0.0f, 2.5f, 0.0f, 80f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.CINEMATIC_MONO -> floatArrayOf(1.5f, 1.5f, 1.5f, 0.0f, -120f, 1.5f, 1.5f, 1.5f, 0.0f, -120f, 1.5f, 1.5f, 1.5f, 0.0f, -120f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.CHROME_CORRUPT -> floatArrayOf(0.8f, 0.8f, 0.8f, 0.0f, -30f, 0.8f, 0.8f, 0.8f, 0.0f, -30f, 0.8f, 0.8f, 0.8f, 0.0f, -30f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.NEON_MATRIX -> floatArrayOf(0.0f, 0.5f, 0.0f, 0.0f, -50f, 0.0f, 2.5f, 0.0f, 0.0f, 20f, 0.0f, 0.5f, 0.0f, 0.0f, -50f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.TOXIC_WASTE -> floatArrayOf(2.0f, 0.5f, 0.0f, 0.0f, 30f, 1.0f, 2.0f, 0.0f, 0.0f, 40f, 0.0f, 0.0f, 0.5f, 0.0f, -40f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.BLOOD_MOON -> floatArrayOf(3.0f, 0.0f, 0.0f, 0.0f, 50f, 0.0f, 0.3f, 0.0f, 0.0f, -50f, 0.0f, 0.0f, 0.3f, 0.0f, -50f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.DEEP_OCEAN -> floatArrayOf(0.2f, 0.0f, 0.0f, 0.0f, -30f, 0.0f, 1.2f, 0.5f, 0.0f, 10f, 0.0f, 0.5f, 2.5f, 0.0f, 50f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.GHOST_MACHINE -> floatArrayOf(0.8f, 0.8f, 1.0f, 0.0f, 50f, 0.8f, 1.0f, 1.0f, 0.0f, 60f, 1.0f, 1.2f, 1.5f, 0.0f, 80f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.GOLDEN_ERA -> floatArrayOf(1.5f, 1.0f, 0.5f, 0.0f, 20f, 1.0f, 1.2f, 0.4f, 0.0f, 10f, 0.5f, 0.6f, 0.3f, 0.0f, -20f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.TERMINAL_AMBER -> floatArrayOf(2.0f, 2.0f, 2.0f, 0.0f, -50f, 1.0f, 1.0f, 1.0f, 0.0f, -50f, 0.0f, 0.0f, 0.0f, 0.0f, -100f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.VAMPIRE_NIGHT -> floatArrayOf(1.8f, 1.8f, 1.8f, 0.0f, -80f, 0.0f, 0.0f, 0.0f, 0.0f, -150f, 0.0f, 0.0f, 0.0f, 0.0f, -150f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.ALIEN_FLORA -> floatArrayOf(1.5f, 0.0f, 1.5f, 0.0f, 20f, 0.0f, 2.5f, 0.0f, 0.0f, 40f, 1.5f, 0.0f, 1.5f, 0.0f, 20f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.CORRUPTED_VHS -> floatArrayOf(1.1f, 0.8f, 0.6f, 0.0f, -10f, 0.6f, 1.2f, 0.7f, 0.0f, 10f, 0.5f, 0.6f, 1.0f, 0.0f, -10f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.ELECTRIC_INDIGO -> floatArrayOf(0.5f, 0.0f, 1.5f, 0.0f, 10f, 0.0f, 0.5f, 1.5f, 0.0f, -10f, 1.0f, 0.5f, 3.0f, 0.0f, 60f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.OVEREXPOSED -> floatArrayOf(1.5f, 0.0f, 0.0f, 0.0f, 80f, 0.0f, 1.5f, 0.0f, 0.0f, 80f, 0.0f, 0.0f, 1.5f, 0.0f, 80f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.UNDERGROUND_CLUB -> floatArrayOf(1.2f, 0.0f, 0.8f, 0.0f, -20f, 0.0f, 0.2f, 0.5f, 0.0f, -40f, 0.5f, 0.2f, 1.8f, 0.0f, 30f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.RADIOACTIVE -> floatArrayOf(0.8f, 1.5f, 0.0f, 0.0f, 10f, 0.0f, 2.5f, 1.0f, 0.0f, 50f, 0.0f, 1.0f, 1.5f, 0.0f, 20f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.COLD_STEEL -> floatArrayOf(0.6f, 0.8f, 1.0f, 0.0f, -40f, 0.6f, 0.9f, 1.2f, 0.0f, -20f, 0.8f, 1.2f, 1.8f, 0.0f, 10f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
        GlitchPreset.VOID_BLACK -> floatArrayOf(1.2f, 1.2f, 1.2f, 0.0f, -180f, 1.2f, 1.2f, 1.2f, 0.0f, -180f, 1.2f, 1.2f, 1.2f, 0.0f, -180f, 0.0f, 0.0f, 0.0f, 1.0f, 0f)
    }
    for (i in 0..14) { if (i % 5 == 4) b[i] = (b[i] * overdrive) + ((overdrive - 1f) * 25f) else b[i] *= overdrive }
    return ColorMatrix(b)
}

fun drawGlitchesOnAndroidCanvas(canvas: android.graphics.Canvas, preset: GlitchPreset, w: Float, h: Float, intensity: Float) {
    if (intensity < 0.05f) return
    val paint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL }
    val s = (w / 1000f) * intensity
    fun drawBar(color: Int, x: Float, yRatio: Float, widthPx: Float, heightPx: Float) {
        val baseAlpha = android.graphics.Color.alpha(color)
        paint.color = android.graphics.Color.argb((baseAlpha * intensity).toInt().coerceIn(0, 255), android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
        canvas.drawRect(x, h * yRatio, x + widthPx, (h * yRatio) + (heightPx * s), paint)
    }
    when (preset) {
        GlitchPreset.MAGENTA_WASH -> { drawBar(android.graphics.Color.BLACK, 0f, 0.15f, w, 15f); drawBar(android.graphics.Color.BLACK, w * 0.2f, 0.6f, w, 35f) }
        GlitchPreset.CYBER_VIOLET -> { drawBar(android.graphics.Color.argb(204, 255, 255, 255), w * 0.1f, 0.3f, w, 4f); drawBar(android.graphics.Color.argb(153, 0, 255, 255), 0f, 0.75f, w * 0.8f, 6f) }
        GlitchPreset.CINEMATIC_MONO -> { drawBar(android.graphics.Color.BLACK, 0f, 0.4f, w * 0.7f, 45f); drawBar(android.graphics.Color.BLACK, w * 0.5f, 0.8f, w, 60f) }
        GlitchPreset.CHROME_CORRUPT -> { drawBar(android.graphics.Color.argb(128, 128, 128, 128), 0f, 0.1f, w, 25f); drawBar(android.graphics.Color.DKGRAY, w * 0.3f, 0.5f, w, 80f) }
        GlitchPreset.NEON_MATRIX -> { drawBar(android.graphics.Color.argb(153, 0, 255, 0), 0f, 0.2f, w, 8f); drawBar(android.graphics.Color.BLACK, w * 0.1f, 0.8f, w, 20f) }
        GlitchPreset.TOXIC_WASTE -> { drawBar(android.graphics.Color.argb(153, 255, 255, 0), 0f, 0.5f, w*0.6f, 12f); drawBar(android.graphics.Color.argb(153, 0, 255, 0), w * 0.4f, 0.9f, w, 5f) }
        GlitchPreset.BLOOD_MOON -> { drawBar(android.graphics.Color.argb(153, 255, 0, 0), w * 0.2f, 0.1f, w, 15f); drawBar(android.graphics.Color.BLACK, 0f, 0.7f, w * 0.8f, 40f) }
        GlitchPreset.DEEP_OCEAN -> { drawBar(android.graphics.Color.argb(153, 0, 255, 255), 0f, 0.35f, w, 6f); drawBar(android.graphics.Color.argb(153, 0, 0, 255), w * 0.3f, 0.65f, w, 18f) }
        GlitchPreset.GHOST_MACHINE -> { drawBar(android.graphics.Color.argb(230, 255, 255, 255), 0f, 0.45f, w, 2f); drawBar(android.graphics.Color.argb(153, 211, 211, 211), w * 0.5f, 0.85f, w, 8f) }
        GlitchPreset.GOLDEN_ERA -> { drawBar(android.graphics.Color.argb(102, 255, 255, 0), 0f, 0.25f, w * 0.7f, 4f); drawBar(android.graphics.Color.argb(153, 0, 0, 0), 0f, 0.55f, w, 10f) }
        GlitchPreset.TERMINAL_AMBER -> { drawBar(android.graphics.Color.argb(153, 255, 165, 0), 0f, 0.15f, w, 3f); drawBar(android.graphics.Color.BLACK, w * 0.2f, 0.75f, w, 25f) }
        GlitchPreset.VAMPIRE_NIGHT -> { drawBar(android.graphics.Color.argb(153, 255, 0, 0), 0f, 0.4f, w, 5f); drawBar(android.graphics.Color.DKGRAY, 0f, 0.9f, w * 0.5f, 30f) }
        GlitchPreset.ALIEN_FLORA -> { drawBar(android.graphics.Color.argb(153, 255, 0, 255), w * 0.1f, 0.3f, w, 10f); drawBar(android.graphics.Color.argb(153, 0, 255, 0), 0f, 0.8f, w * 0.6f, 15f) }
        GlitchPreset.CORRUPTED_VHS -> { drawBar(android.graphics.Color.argb(76, 128, 128, 128), 0f, 0.5f, w, 40f); drawBar(android.graphics.Color.argb(204, 255, 255, 255), 0f, 0.95f, w, 4f) }
        GlitchPreset.ELECTRIC_INDIGO -> { drawBar(android.graphics.Color.argb(153, 0, 0, 255), w * 0.3f, 0.2f, w, 12f); drawBar(android.graphics.Color.argb(153, 255, 0, 255), 0f, 0.6f, w * 0.7f, 6f) }
        GlitchPreset.OVEREXPOSED -> { drawBar(android.graphics.Color.argb(178, 255, 255, 255), 0f, 0.1f, w, 30f); drawBar(android.graphics.Color.BLACK, w * 0.4f, 0.7f, w, 8f) }
        GlitchPreset.UNDERGROUND_CLUB -> { drawBar(android.graphics.Color.argb(153, 64, 64, 64), 0f, 0.4f, w, 50f); drawBar(android.graphics.Color.argb(153, 255, 0, 255), w * 0.2f, 0.8f, w, 15f) }
        GlitchPreset.RADIOACTIVE -> { drawBar(android.graphics.Color.argb(153, 0, 255, 255), 0f, 0.25f, w * 0.8f, 10f); drawBar(android.graphics.Color.argb(153, 0, 255, 0), w * 0.5f, 0.75f, w, 20f) }
        GlitchPreset.COLD_STEEL -> { drawBar(android.graphics.Color.argb(153, 211, 211, 211), 0f, 0.3f, w, 5f); drawBar(android.graphics.Color.BLACK, 0f, 0.85f, w * 0.6f, 35f) }
        GlitchPreset.VOID_BLACK -> { drawBar(android.graphics.Color.argb(153, 255, 255, 255), w * 0.1f, 0.5f, w, 2f); drawBar(android.graphics.Color.DKGRAY, 0f, 0.9f, w, 10f) }
    }
}

fun DrawScope.drawGlitchesForPreset(preset: GlitchPreset, w: Float, h: Float, intensity: Float) {
    if (intensity < 0.05f) return
    val a = 0.6f * intensity; val i = intensity
    when (preset) {
        GlitchPreset.MAGENTA_WASH -> { drawRect(Color.Black, Offset(0f, h * 0.15f), Size(w, 15f*i)); drawRect(Color.Black, Offset(w * 0.2f, h * 0.6f), Size(w, 35f*i)) }
        GlitchPreset.CYBER_VIOLET -> { drawRect(Color.White, Offset(w * 0.1f, h * 0.3f), Size(w, 4f*i), alpha = 0.8f*intensity); drawRect(Color.Cyan, Offset(0f, h * 0.75f), Size(w * 0.8f, 6f*i), alpha = 0.6f*intensity) }
        GlitchPreset.CINEMATIC_MONO -> { drawRect(Color.Black, Offset(0f, h * 0.4f), Size(w * 0.7f, 45f*i)); drawRect(Color.Black, Offset(w * 0.5f, h * 0.8f), Size(w, 60f*i)) }
        GlitchPreset.CHROME_CORRUPT -> { drawRect(Color.Gray, Offset(0f, h * 0.1f), Size(w, 25f*i), alpha = 0.5f*intensity); drawRect(Color.DarkGray, Offset(w * 0.3f, h * 0.5f), Size(w, 80f*i)) }
        GlitchPreset.NEON_MATRIX -> { drawRect(Color.Green, Offset(0f, h * 0.2f), Size(w, 8f*i), alpha = a); drawRect(Color.Black, Offset(w * 0.1f, h * 0.8f), Size(w, 20f*i)) }
        GlitchPreset.TOXIC_WASTE -> { drawRect(Color.Yellow, Offset(0f, h * 0.5f), Size(w*0.6f, 12f*i), alpha = a); drawRect(Color.Green, Offset(w * 0.4f, h * 0.9f), Size(w, 5f*i), alpha = a) }
        GlitchPreset.BLOOD_MOON -> { drawRect(Color.Red, Offset(w * 0.2f, h * 0.1f), Size(w, 15f*i), alpha = a); drawRect(Color.Black, Offset(0f, h * 0.7f), Size(w * 0.8f, 40f*i)) }
        GlitchPreset.DEEP_OCEAN -> { drawRect(Color.Cyan, Offset(0f, h * 0.35f), Size(w, 6f*i), alpha = a); drawRect(Color.Blue, Offset(w * 0.3f, h * 0.65f), Size(w, 18f*i), alpha = a) }
        GlitchPreset.GHOST_MACHINE -> { drawRect(Color.White, Offset(0f, h * 0.45f), Size(w, 2f*i), alpha = 0.9f*intensity); drawRect(Color.LightGray, Offset(w * 0.5f, h * 0.85f), Size(w, 8f*i), alpha = a) }
        GlitchPreset.GOLDEN_ERA -> { drawRect(Color.Yellow, Offset(0f, h * 0.25f), Size(w * 0.7f, 4f*i), alpha = 0.4f*intensity); drawRect(Color.Black, Offset(0f, h * 0.55f), Size(w, 10f*i), alpha = a) }
        GlitchPreset.TERMINAL_AMBER -> { drawRect(Color(0xFFFFA500), Offset(0f, h * 0.15f), Size(w, 3f*i), alpha = a); drawRect(Color.Black, Offset(w * 0.2f, h * 0.75f), Size(w, 25f*i)) }
        GlitchPreset.VAMPIRE_NIGHT -> { drawRect(Color.Red, Offset(0f, h * 0.4f), Size(w, 5f*i), alpha = a); drawRect(Color.DarkGray, Offset(0f, h * 0.9f), Size(w * 0.5f, 30f*i)) }
        GlitchPreset.ALIEN_FLORA -> { drawRect(Color.Magenta, Offset(w * 0.1f, h * 0.3f), Size(w, 10f*i), alpha = a); drawRect(Color.Green, Offset(0f, h * 0.8f), Size(w * 0.6f, 15f*i), alpha = a) }
        GlitchPreset.CORRUPTED_VHS -> { drawRect(Color.Gray, Offset(0f, h * 0.5f), Size(w, 40f*i), alpha = 0.3f*intensity); drawRect(Color.White, Offset(0f, h * 0.95f), Size(w, 4f*i), alpha = 0.8f*intensity) }
        GlitchPreset.ELECTRIC_INDIGO -> { drawRect(Color.Blue, Offset(w * 0.3f, h * 0.2f), Size(w, 12f*i), alpha = a); drawRect(Color.Magenta, Offset(0f, h * 0.6f), Size(w * 0.7f, 6f*i), alpha = a) }
        GlitchPreset.OVEREXPOSED -> { drawRect(Color.White, Offset(0f, h * 0.1f), Size(w, 30f*i), alpha = 0.7f*intensity); drawRect(Color.Black, Offset(w * 0.4f, h * 0.7f), Size(w, 8f*i)) }
        GlitchPreset.UNDERGROUND_CLUB -> { drawRect(Color.DarkGray, Offset(0f, h * 0.4f), Size(w, 50f*i), alpha = a); drawRect(Color.Magenta, Offset(w * 0.2f, h * 0.8f), Size(w, 15f*i), alpha = a) }
        GlitchPreset.RADIOACTIVE -> { drawRect(Color.Cyan, Offset(0f, h * 0.25f), Size(w * 0.8f, 10f*i), alpha = a); drawRect(Color.Green, Offset(w * 0.5f, h * 0.75f), Size(w, 20f*i), alpha = a) }
        GlitchPreset.COLD_STEEL -> { drawRect(Color.LightGray, Offset(0f, h * 0.3f), Size(w, 5f*i), alpha = a); drawRect(Color.Black, Offset(0f, h * 0.85f), Size(w * 0.6f, 35f*i)) }
        GlitchPreset.VOID_BLACK -> { drawRect(Color.White, Offset(w * 0.1f, h * 0.5f), Size(w, 2f*i), alpha = a); drawRect(Color.DarkGray, Offset(0f, h * 0.9f), Size(w, 10f*i)) }
    }
}