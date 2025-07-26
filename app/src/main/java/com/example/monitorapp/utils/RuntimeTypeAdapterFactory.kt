import com.example.monitorapp.commandCenter.RemoteCommand
import com.example.monitorapp.utils.AppActionEnum
import com.google.gson.*
import java.lang.reflect.Type

class RemoteCommandAdapter : JsonSerializer<RemoteCommand>, JsonDeserializer<RemoteCommand> {

    override fun serialize(
        src: RemoteCommand,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()

        // Add common type field
        jsonObject.addProperty("type", src.type)

        // Add specific fields based on the type
        when (src) {
            is RemoteCommand.Tap -> {
                jsonObject.addProperty("x", src.x)
                jsonObject.addProperty("y", src.y)
            }

            is RemoteCommand.Swipe -> {
                jsonObject.addProperty("startX", src.startX)
                jsonObject.addProperty("startY", src.startY)
                jsonObject.addProperty("endX", src.endX)
                jsonObject.addProperty("endY", src.endY)
                jsonObject.addProperty("duration", src.duration)
            }

            is RemoteCommand.InputText -> {
                jsonObject.addProperty("text", src.text)
            }

            is RemoteCommand.AppGlobalActions -> {
                jsonObject.addProperty("appAction", src.appAction.value)
                // no additional fields
            }

            else -> {}
        }

        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): RemoteCommand {
        val obj = json.asJsonObject
        val type = obj["type"]?.asString ?: throw JsonParseException("Missing 'type' field")

        return when (type) {
            "tap" -> RemoteCommand.Tap(
                x = obj["x"].asFloat,
                y = obj["y"].asFloat
            )

            "swipe" -> RemoteCommand.Swipe(
                startX = obj["startX"].asFloat,
                startY = obj["startY"].asFloat,
                endX = obj["endX"].asFloat,
                endY = obj["endY"].asFloat,
                duration = obj["duration"]?.asLong ?: 300L
            )

            "input_text" -> RemoteCommand.InputText(
                text = obj["text"].asString
            )

            "launch_app_drawer" -> RemoteCommand.AppGlobalActions(
                appAction = AppActionEnum.fromValue(obj["appAction"].asString) ?: AppActionEnum.appActionHome
            )

            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}
