package com.guardian.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ClassificationResult(
    val isExplicit: Boolean,
    val confidence: Float,
    val label: String
)

@Singleton
class ImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MODEL_FILENAME = "guardian_classifier.tflite"
        private const val INPUT_SIZE = 224
        private const val EXPLICIT_THRESHOLD = 0.72f
        private val NSFW_LABELS = setOf(
            "Pornography", "Explicit", "Nudity", "Underwear", "Swimwear", "Bikini", "Lingerie"
        )
        private const val MLKIT_THRESHOLD = 0.75f
    }

    private val interpreter: Interpreter? by lazy {
        try {
            val model = loadModelFile()
            Interpreter(model, Interpreter.Options().apply { numThreads = 2 })
        } catch (_: Exception) {
            null
        }
    }

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    suspend fun classify(bitmap: Bitmap): ClassificationResult = withContext(Dispatchers.IO) {
        interpreter?.let { classifyWithTFLite(it, bitmap) } ?: classifyWithMlKit(bitmap)
    }

    suspend fun classifyUri(uri: Uri): ClassificationResult = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
                ?: return@withContext ClassificationResult(false, 0f, "unreadable")
            classify(bitmap)
        } ?: ClassificationResult(false, 0f, "unreadable")
    }

    private fun classifyWithTFLite(interp: Interpreter, bitmap: Bitmap): ClassificationResult {
        val processed = imageProcessor.process(TensorImage.fromBitmap(bitmap))
        val output = Array(1) { FloatArray(2) }
        interp.run(processed.buffer, output)
        val explicitScore = output[0][1]
        val isExplicit = explicitScore >= EXPLICIT_THRESHOLD
        return ClassificationResult(
            isExplicit = isExplicit,
            confidence = if (isExplicit) explicitScore else output[0][0],
            label = if (isExplicit) "explicit" else "safe"
        )
    }

    private suspend fun classifyWithMlKit(bitmap: Bitmap): ClassificationResult =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val hit = labels.firstOrNull { label ->
                        NSFW_LABELS.any { label.text.contains(it, ignoreCase = true) } &&
                            label.confidence >= MLKIT_THRESHOLD
                    }
                    if (hit != null) {
                        cont.resume(
                            ClassificationResult(true, hit.confidence, hit.text)
                        )
                    } else {
                        cont.resume(ClassificationResult(false, 1f, "safe"))
                    }
                    labeler.close()
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                    labeler.close()
                }
        }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFd = context.assets.openFd(MODEL_FILENAME)
        FileInputStream(assetFd.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFd.startOffset,
                assetFd.declaredLength
            )
        }
    }
}
