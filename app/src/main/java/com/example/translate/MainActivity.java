package com.example.translate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private EditText etFromText;
    private Spinner spTo;
    private Button btnTranslate;
    private TextView tvResult;
    private String toLangCode = "auto";//目标语言
    private final Map<String, String> langMap = new HashMap<>() {{
        put("自动检测", "auto");
        put("中文", "zh-CHS");
        put("英语", "en");
        put("日语", "ja");
        put("韩语", "ko");
        put("法语", "fr");
        put("德语", "de");
    }};
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
        initClick();
        initLangSpinner();
    }
    private void initView() {
        etFromText = findViewById(R.id.et_from_text);
        spTo = findViewById(R.id.sp_to);
        btnTranslate = findViewById(R.id.btn_translate);
        tvResult = findViewById(R.id.tv_result);
    }
    private void initLangSpinner() {
        String[] langNames = langMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, langNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(adapter);
        spTo.setSelection(4);
        spTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = (String) parent.getItemAtPosition(position);
                toLangCode = langMap.get(selectedLang);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void initClick() {
        btnTranslate.setOnClickListener(v -> {
            String fromText = etFromText.getText().toString().trim();
            if (fromText.isEmpty()) {
                Toast.makeText(MainActivity.this, "请输入文本", Toast.LENGTH_SHORT).show();
                return;
            }
            translateText(fromText, toLangCode);
        });
    }
    private void translateText(String text, String to) {
        try {
            String url = String.format(
                    "https://60s.viki.moe/v2/fanyi?text=%s&from=auto&to=%s&encoding=json",
                    text, to
            );
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> {
                        Toast.makeText(MainActivity.this, "翻译失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d("translate", "onFailure: translate");
                        tvResult.setText("");
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseStr = response.body().string();
                        Log.d("RESPONSE", responseStr); //查看返回数据
                        JsonData jsonData=new JsonData();
                        try {
                            JSONObject rootObj = new JSONObject(responseStr);
                            jsonData.setCode(rootObj.getInt("code"));
                            if (jsonData.getCode() == 200) {
                                jsonData.setMessage(rootObj.getString("message"));
                                JSONObject data = rootObj.getJSONObject("data");
                                JSONObject source = data.getJSONObject("source");
                                JSONObject target = data.getJSONObject("target");
                                Data data1=new Data();
                                Source source1=new Source();
                                Target target1=new Target();
                                source1.setText(source.getString("text"));
                                source1.setType(source.getString("type"));
                                source1.setTypeDesc(source.getString("type_desc"));
                                source1.setPronounce(source.getString("pronounce"));
                                target1.setText(target.getString("text"));
                                target1.setType(target.getString("type"));
                                target1.setTypeDesc(target.getString("type_desc"));
                                target1.setPronounce(target.getString("pronounce"));
                                data1.setSource(source1);
                                data1.setTarget(target1);
                                jsonData.setData(data1);
                                String showResult = String.format(
                                        "源语言(%s)：%s\n翻译结果：%s",
                                        jsonData.getData().getSource().getType(), jsonData.getData().getSource().getText(),jsonData.getData().getTarget().getText()
                                );
                                mainHandler.post(() -> tvResult.setText(showResult));
                            } else {
                                String errorMsg = rootObj.optString("message", "请求失败");
                                mainHandler.post(() -> {
                                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                    Log.d("decode", errorMsg);
                                    tvResult.setText("");
                                });
                            }
                        } catch (JSONException e) {
                            mainHandler.post(() -> {
                                Toast.makeText(MainActivity.this, "解析结果失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.d("decode", String.valueOf(e));
                                tvResult.setText("");
                            });
                        }
                    } else {
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this, "请求失败，状态码：" + response.code(), Toast.LENGTH_SHORT).show();
                            Log.d("decode", "请求失败，状态码：" +response.code());
                            tvResult.setText("");
                        });
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                Toast.makeText(MainActivity.this, "参数编码失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("decode", String.valueOf(e));
                tvResult.setText("");
            });
        }
    }
}