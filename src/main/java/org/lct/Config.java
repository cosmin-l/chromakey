package org.lct;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;

class Config {
    private String cfgPath;
    float minHue, maxHue, minSat, maxSat, minBri, maxBri,
            hueDelta, satDelta, briDelta;
    String camName;
    int resWidth, resHeight;


    Config(String cfgPath) throws IOException, ParseException, IllegalAccessException {
        this.cfgPath = cfgPath;
        FileReader reader = new FileReader(cfgPath);
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        Field[] fields = Config.class.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isPrivate(field.getModifiers())) {
                field.setAccessible(true);
                if (field.getType() == float.class) field.setFloat(this, ((Number) jsonObject.get(field.getName())).floatValue());
                if (field.getType() == int.class) field.setInt(this, ((Number) jsonObject.get(field.getName())).intValue());
                if (field.getType() == String.class) field.set(this, jsonObject.get(field.getName()));
            }
        }
    }

    void writeConfig() {
        JSONObject configObj = new JSONObject();
        try {
            Field[] fields = Config.class.getDeclaredFields();
            for (Field field : fields) {
                if (!Modifier.isPrivate(field.getModifiers())) {
                    field.setAccessible(true);
                    configObj.put(field.getName(), field.get(this));
                }
            }
            Files.write(Paths.get(cfgPath), configObj.toJSONString().getBytes());
        } catch (Exception e) {
            System.out.println("Error writing config file: " + e.getMessage());
        }
    }
}
