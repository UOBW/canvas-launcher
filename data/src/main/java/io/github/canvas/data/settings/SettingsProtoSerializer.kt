package io.github.canvas.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.canvas.data.proto.SettingsProto
import java.io.InputStream
import java.io.OutputStream

internal object SettingsProtoSerializer : Serializer<SettingsProto> {
    override val defaultValue: SettingsProto get() = SettingsProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SettingsProto = try {
        SettingsProto.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Failed to read settings proto", exception)
    }

    override suspend fun writeTo(t: SettingsProto, output: OutputStream): Unit = t.writeTo(output)
}
