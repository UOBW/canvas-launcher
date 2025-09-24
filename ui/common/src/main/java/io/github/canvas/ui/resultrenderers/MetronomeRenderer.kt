package io.github.canvas.ui.resultrenderers

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.canvas.data.math.MetronomeParser
import io.github.canvas.ui.MetronomeSearchResult
import io.github.canvas.ui.R
import io.github.canvas.ui.SimpleInteractionSource
import io.github.canvas.ui.log
import io.github.canvas.ui.verticalSearchResultPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val SAMPLE_RATE: Int = 48_000 // 48 KHz
private const val SAMPLE_COUNT: Int = SAMPLE_RATE / 16 // 1 click = 1/16 of a second = 3000 samples
private const val AMPLITUDE: Int = 127 // max
private const val FREQUENCY_DOWNBEAT: Double = 440.0 // 440 Hz, A4
private const val FREQUENCY_OFFBEAT: Double = 293.665 // 293.665 Hz, D4, a fifth below A4
private const val FADE_OUT_DURATION: Double = SAMPLE_COUNT / 6.0

private fun createTrack(): AudioTrack = AudioTrack(
    /* attributes = */ AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
        .build(),
    /* format = */ AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build(),
    /* bufferSizeInBytes = */ SAMPLE_COUNT,
    /* mode = */ AudioTrack.MODE_STATIC,
    /* sessionId = */ AudioManager.AUDIO_SESSION_ID_GENERATE
)

private fun generateData(frequency: Double): ByteArray = ByteArray(SAMPLE_COUNT) { t ->
    // y(t) = y₀ * sin(2πft) -- y₀ = amplitude, f = frequency, t = time
    var d = AMPLITUDE * sin(2.0 * PI * frequency * (t.toDouble() / SAMPLE_RATE.toDouble()))
    if (t >= SAMPLE_COUNT - FADE_OUT_DURATION) {
        d *= ((SAMPLE_COUNT - t) / FADE_OUT_DURATION)
    }
    (d.toInt() + 128).toByte()
}


@Composable
fun MetronomeRenderer(
    searchResult: MetronomeSearchResult,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    beforeClick: (() -> Unit)?,
    interactionSource: SimpleInteractionSource?,
) {
    val metronome = searchResult.metronome

    var audioTrackDownbeat: AudioTrack? by remember { mutableStateOf(null) } // beat 1
    var audioTrackOffbeat: AudioTrack? by remember { mutableStateOf(null) } // other beats

    var paused by rememberSaveable { mutableStateOf(false) }
    var beat by remember(audioTrackDownbeat, audioTrackOffbeat) {
        mutableIntStateOf(-1) // The beat playing right at this very moment, or -1
    }
    var nextBeat by remember(audioTrackDownbeat, audioTrackOffbeat) { mutableIntStateOf(0) }

    fun setPaused(value: Boolean) {
        beforeClick?.invoke()
        if (onClick != null) {
            onClick()
        } else {
            paused = value
            beat = -1
            nextBeat = 0
        }
    }

    DisposableEffect(metronome) {
        val trackDownbeat = createTrack()
        val trackOffbeat = if (metronome.beats != 1) createTrack() else null

        val dataDownbeat = generateData(FREQUENCY_DOWNBEAT)
        val dataOffbeat = if (trackOffbeat != null) generateData(FREQUENCY_OFFBEAT) else null

        trackDownbeat.write(dataDownbeat, 0, dataDownbeat.size)
        trackOffbeat?.write(dataOffbeat!!, 0, dataOffbeat.size)
        audioTrackDownbeat = trackDownbeat
        audioTrackOffbeat = trackOffbeat

        onDispose {
            audioTrackDownbeat?.release()
            audioTrackOffbeat?.release()
        }
    }

    LaunchedEffect(audioTrackDownbeat, audioTrackOffbeat, paused) {
        if (audioTrackDownbeat == null || paused) return@LaunchedEffect

        val delay = (1.minutes / metronome.bpm).inWholeMilliseconds.coerceAtLeast(1)
        val soundLength = (SAMPLE_COUNT.toDouble() / SAMPLE_RATE).seconds

        try {
            withContext(Dispatchers.IO) {
                while (true) {
                    launch {
                        (if (nextBeat == 0) audioTrackDownbeat else audioTrackOffbeat).let { track ->
                            track!!.pause()
                            track.playbackHeadPosition = 0
                            track.play()
                        }
                        beat = nextBeat
                        delay(soundLength)
                        beat = -1
                        nextBeat = (nextBeat + 1) % metronome.beats
                    }
                    delay(delay)
                }
            }
        } catch (e: IllegalStateException) {
            log.e("Failed to play metronome", e)
        }
    }

    Card(
        modifier = modifier
            .padding(
                vertical = verticalSearchResultPadding,
                horizontal = verticalSearchResultPadding * 2
            )
            .combinedClickable(
                onClick = { setPaused(!paused) },
                onLongClick = onLongClick,
            )
    ) {
        ListItem(
            leadingContent = {
                IconToggleButton(
                    checked = !paused,
                    onCheckedChange = { setPaused(!it) }
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = stringResource(if (paused) R.string.accessibility_metronome_resume else R.string.accessibility_metronome_pause),
                    )
                }
            },
            headlineContent = { Text(stringResource(R.string.metronome_bpm, metronome.bpm)) },
            supportingContent = { Text(stringResource(MetronomeParser.bpmToTempoName(metronome.bpm))) },
            trailingContent = {
                val color = MaterialTheme.colorScheme.primary
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 4,
                    modifier = Modifier.Companion.padding(6.dp)
                ) {
                    for (i in 0..(metronome.beats - 1)) {
                        Canvas(
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            drawCircle(
                                color.copy(
                                    alpha = when {
                                        beat != i -> 0.25f
                                        beat == 0 -> 1f
                                        else -> 0.75f
                                    }
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(interactionSource) {
        interactionSource?.collect { setPaused(!paused) }
    }
}
