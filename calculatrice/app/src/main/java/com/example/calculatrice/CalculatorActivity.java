package com.example.calculatrice;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CalculatorActivity extends AppCompatActivity {

    EditText number1, number2;
    TextView tvResult;
    Button btnAdd, btnSub, btnMul, btnReset, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        number1 = findViewById(R.id.number1);
        number2 = findViewById(R.id.number2);
        tvResult = findViewById(R.id.tvResult);
        btnAdd = findViewById(R.id.btnAdd);
        btnSub = findViewById(R.id.btnSub);
        btnMul = findViewById(R.id.btnMul);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);

        btnAdd.setOnClickListener(v -> calculate("+"));
        btnSub.setOnClickListener(v -> calculate("-"));
        btnMul.setOnClickListener(v -> calculate("*"));
        btnReset.setOnClickListener(v -> resetFields());
        btnBack.setOnClickListener(v -> finish()); // Back to homepage
    }

    private void calculate(String op) {
        String s1 = number1.getText().toString().trim();
        String s2 = number2.getText().toString().trim();

        if (TextUtils.isEmpty(s1) || TextUtils.isEmpty(s2)) {
            Toast.makeText(this, "Veuillez entrer les deux nombres", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double n1 = Double.parseDouble(s1);
            double n2 = Double.parseDouble(s2);
            double result = 0;

            switch (op) {
                case "+": result = n1 + n2; break;
                case "-": result = n1 - n2; break;
                case "*": result = n1 * n2; break;
            }

            tvResult.setText("Résultat : " + result);

            new AlertDialog.Builder(this)
                    .setTitle("Résultat de l'opération")
                    .setMessage(n1 + " " + op + " " + n2 + " = " + result)
                    .setPositiveButton("OK", null)
                    .show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valeur non numérique détectée", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetFields() {
        number1.setText("");
        number2.setText("");
        tvResult.setText("Résultat :");
    }
}
