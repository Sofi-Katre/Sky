package com.example.sky;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;

public class Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        // 1. Находим View для данных пользователя
        TextView userName = findViewById(R.id.UserName);
        TextView userEmail = findViewById(R.id.userEmail);
        ImageView userPhoto = findViewById(R.id.userPhoto);

        // 2. Получаем данные последнего вошедшего аккаунта
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            userName.setText(acct.getDisplayName());
            userEmail.setText(acct.getEmail());
            Uri personPhoto = acct.getPhotoUrl();

            Glide.with(this)
                    .load(personPhoto)
                    .placeholder(R.drawable.zaglushka)
                    .into(userPhoto);
        }

        Button btnSignOut = findViewById(R.id.btnSignOut);

        btnSignOut.setOnClickListener(v -> {
            // 1. Выход из Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. Выход из Google
            GoogleSignIn.getClient(this,
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .build())
                    .signOut()
                    .addOnCompleteListener(task -> {
                        // 3. Переход на MainActivity после выхода
                        Intent intent = new Intent(Profile.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
        });

        // --- Навигация ---
        ImageView btnSleep = findViewById(R.id.btnSleep);
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        btnSleep.setOnClickListener(v -> startActivity(new Intent(Profile.this, Books.class)));
        btnWhether.setOnClickListener(v -> startActivity(new Intent(Profile.this, entryPage.class)));
        // btnProfile — мы уже здесь, клика не делаем
    }
}