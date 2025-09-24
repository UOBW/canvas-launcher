@file:OptIn(ExperimentalContracts::class)

package io.github.canvas.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal val log: Logger = Logger("io.github.canvas.data")

/** Shorthand to access the LauncherApps system service associated with the context */
internal val Context.launcherApps: LauncherApps
    get() = getSystemService<LauncherApps>()!!

internal abstract class ContentObserverAdapter : ContentObserver(null) {
    abstract fun onChange()

    override fun onChange(selfChange: Boolean): Unit = onChange()
    override fun onChange(selfChange: Boolean, uri: Uri?): Unit = onChange()
    override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int): Unit = onChange()
    override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int): Unit = onChange()
}

/** Immediately emits a Unit and then continues emitting Unit every time an app is installed, uninstalled, updated, etc.  */
internal val LauncherApps.listener: ListenerFlow
    get() = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String?>?, user: UserHandle?, replacing: Boolean,
            ) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String?>?, user: UserHandle?, replacing: Boolean,
            ) {
                trySend(Unit)
            }
        }
        registerCallback(callback, Handler(Looper.getMainLooper()))
        //Initial value
        trySend(Unit)
        awaitClose { unregisterCallback(callback) }
    }

internal suspend fun PackageManager.isPackageInstalled(packageName: String): Boolean = try {
    io { getApplicationInfo(packageName, 0) }.enabled
} catch (_: PackageManager.NameNotFoundException) {
    false
}

fun repositoryCoroutineScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return withContext(Dispatchers.IO, block)
}

internal suspend fun <T, R> T.io(block: suspend T.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return withContext(Dispatchers.IO) { block() }
}

internal suspend inline fun <T : Closeable?, R> T.useOnIo(crossinline block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return withContext(Dispatchers.IO) { use(block) }
}

internal suspend fun <T> background(block: suspend CoroutineScope.() -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return withContext(Dispatchers.Default, block)
}

inline fun <T, K> Iterable<T>.sortedBy(
    comparator: Comparator<in K>, crossinline selector: (T) -> K,
): List<T> = sortedWith(compareBy(comparator, selector))

inline fun <T, K> MutableList<T>.sortBy(
    comparator: Comparator<in K>, crossinline selector: (T) -> K,
): Unit = sortWith(compareBy(comparator, selector))

/** Tests whether all the bits that are 1 in the flag are also 1 in the int. Additional bits may be 1 in the int but not in the flag. */
infix fun Int.hasFlagSet(flag: Int): Boolean = (this and flag) > 0

suspend fun <T> Flow<T?>.firstNonNull(): T = first { it != null }!!

internal fun @receiver:ColorInt Int.toComposeColor(): Color = Color(this)

internal val ComponentInfo.componentName: ComponentName get() = ComponentName(packageName, name)

// Not intended for data storage, only for short-term usage, e.g., in flows
internal data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
internal data class Quintuple<A, B, C, D, E>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E,
)

// Implementations of the combine() function that allow more than 5 flows, from https://stackoverflow.com/a/73130632
inline fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> {
    return combine(flow1, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5, args[5] as T6
        )
    }
}

inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow1: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, flow7: Flow<T7>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> {
    return combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5,
            args[5] as T6, args[6] as T7
        )
    }
}

inline fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    flow1: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, flow7: Flow<T7>, flow8: Flow<T8>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> {
    return combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5,
            args[5] as T6, args[6] as T7, args[7] as T8
        )
    }
}

inline fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> combine(
    flow1: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, flow7: Flow<T7>, flow8: Flow<T8>, flow9: Flow<T9>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R,
): Flow<R> {
    return combine(
        flow1, flow2, flow3, flow4, flow5, flow6, flow7, flow8, flow9
    ) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5,
            args[5] as T6, args[6] as T7, args[7] as T8, args[8] as T9
        )
    }
}
