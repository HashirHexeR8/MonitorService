import com.example.monitorapp.model.RemoteCommandWrapper
import com.example.monitorapp.commandCenter.RemoteCommand
import com.example.monitorapp.utils.AppActionEnum
import com.google.gson.*
import java.lang.reflect.Type

class RemoteCommandWrapperAdapter : JsonSerializer<RemoteCommandWrapper>, JsonDeserializer<RemoteCommandWrapper> {

    override fun serialize(
        src: RemoteCommandWrapper,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()
        
        // Add the command type
        jsonObject.addProperty("type", "test") // or whatever type you want
        
        // Add the specific command object based on the command type
        when (val command = src.command) {
            is RemoteCommand.Tap -> {
                val tapObject = JsonObject()
                tapObject.addProperty("x", command.x)
                tapObject.addProperty("y", command.y)
                jsonObject.add("Tap", tapObject)
            }
            
            is RemoteCommand.Swipe -> {
                val swipeObject = JsonObject()
                swipeObject.addProperty("startX", command.startX)
                swipeObject.addProperty("startY", command.startY)
                swipeObject.addProperty("endX", command.endX)
                swipeObject.addProperty("endY", command.endY)
                swipeObject.addProperty("duration", command.duration)
                jsonObject.add("Swipe", swipeObject)
            }
            
            is RemoteCommand.InputText -> {
                val inputObject = JsonObject()
                inputObject.addProperty("text", command.text)
                jsonObject.add("InputText", inputObject)
            }
            
            is RemoteCommand.AppGlobalActions -> {
                val launchObject = JsonObject()
                jsonObject.addProperty("appAction", command.appAction.value)
                jsonObject.add("AppGlobalActions", launchObject)
            }
        }
        
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): RemoteCommandWrapper {
        val obj = json.asJsonObject
        
        // Handle the nested structure: { command: { type: "tap", Tap: { x: 500, y: 1000 } } }
        if (obj.has("command")) {
            val commandObj = obj.getAsJsonObject("command")
            
            // Check for Tap command
            if (commandObj.has("Tap")) {
                val tapObj = commandObj.getAsJsonObject("Tap")
                val command = RemoteCommand.Tap(
                    x = tapObj["x"].asFloat,
                    y = tapObj["y"].asFloat
                )
                return RemoteCommandWrapper(command)
            }
            
            // Check for Swipe command
            if (commandObj.has("Swipe")) {
                val swipeObj = commandObj.getAsJsonObject("Swipe")
                val command = RemoteCommand.Swipe(
                    startX = swipeObj["startX"].asFloat,
                    startY = swipeObj["startY"].asFloat,
                    endX = swipeObj["endX"].asFloat,
                    endY = swipeObj["endY"].asFloat,
                    duration = swipeObj["duration"]?.asLong ?: 300L
                )
                return RemoteCommandWrapper(command)
            }
            
            // Check for InputText command
            if (commandObj.has("InputText")) {
                val inputObj = commandObj.getAsJsonObject("InputText")
                val command = RemoteCommand.InputText(
                    text = inputObj["text"].asString
                )
                return RemoteCommandWrapper(command)
            }
            
            // Check for LaunchAppDrawer command
            if (commandObj.has("AppGlobalActions")) {
                val launchObj = commandObj.getAsJsonObject("AppGlobalActions")
                val command = RemoteCommand.AppGlobalActions(
                    appAction = AppActionEnum.fromValue(launchObj["appAction"].asString) ?: AppActionEnum.appActionHome
                )
                return RemoteCommandWrapper(command)
            }
            
            throw JsonParseException("Unknown command type in command object: ${commandObj.keySet()}")
        }
        
        throw JsonParseException("Missing 'command' field in wrapper: ${obj.keySet()}")
    }
} 