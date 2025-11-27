package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class entryPage extends AppCompatActivity {

    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private ImageView btnToDay;
    private ImageView btnAWeek;
    private boolean isTodayActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_page);

        // Находим кнопки по их ID из XML
        ImageView btnSleep = findViewById(R.id.btnSleep);
        ImageView btnMusic = findViewById(R.id.btnMusic);
        ImageView btnProfile = findViewById(R.id.btnProfile);
        // ImageView btnWhether = findViewById(R.id.btnWhether); // Активная кнопка на текущей странице

        // Устанавливаем обработчики нажатий (Listeners) для НАВИГАЦИИ

        // Переход на страницу Books (Books.java)
        btnSleep.setOnClickListener(v -> {
            Intent intent = new Intent(entryPage.this, Books.class);
            startActivity(intent);
        });

        // Переход на страницу Profile (Profile.java)
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(entryPage.this, Profile.class);
            startActivity(intent);
        });

        // *** ИСПРАВЛЕНИЕ: Переход на страницу Music (Music.java) ***
        // Теперь при нажатии на кнопку мы переходим в новое окно, а не управляем шторкой
        btnMusic.setOnClickListener(v -> {
            Intent intent = new Intent(entryPage.this, Music.class);
            startActivity(intent);
        });


//        // 1. Получаем размеры экрана устройства (DisplayMetrics)
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int screenHeight = displayMetrics.heightPixels;
//
//        int sheetHeight = (int) (screenHeight * 0.75);

//        // 3. Находим вашу шторку по ID
//        NestedScrollView bottomSheet = findViewById(R.id.bottom_sheet);
//
//        // Устанавливаем параметры высоты макета для шторки
//        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
//        layoutParams.height = sheetHeight;
//        bottomSheet.setLayoutParams(layoutParams);
//
//        // Получаем объект поведения Bottom Sheet для этого View
//        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
//
//        // 4. Вычисляем минимальную высоту шторки в пикселях
//        int peekHeightInPixels = (int) (screenHeight * 0.40);
//        bottomSheetBehavior.setPeekHeight(peekHeightInPixels);
//
//        // Устанавливаем начальное состояние шторки как свернутое
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//
//        // *** НОВОЕ: Добавляем Callback для отслеживания движения шторки пользователем ***
//        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
//            @Override
//            public void onStateChanged(View bottomSheet, int newState) {
//                // Этот метод вызывается, когда состояние шторки меняется (свернута, развернута, скрыта и т.д.)
//                // Здесь вы можете добавить логику, если нужно что-то делать при достижении определенного состояния
//            }
//
//            @Override
//            public void onSlide(View bottomSheet, float slideOffset) {
//                // Этот метод вызывается каждый раз, когда шторка двигается (пользователь тянет ее)
//                // Здесь вы можете реагировать на движение (например, менять прозрачность фона)
//                // slideOffset меняется от 0 (свернута) до 1 (развернута)
//                // В вашем случае, если вы просто хотите, чтобы она двигалась, ничего писать тут не нужно -
//                // само наличие BottomSheetBehavior уже обеспечивает это движение.
//            }
//        });


//        //ОБРАБОТЧИК КНОПОК С ПОГОДОЙ НА ДЕНЬ И НА НЕДЕЛЮ (без изменений)
//
//        // Находим наши ImageView по ID
//        btnToDay = findViewById(R.id.btnToDay);
//        btnAWeek = findViewById(R.id.btnAWeek);
//
//        setActiveButton(isTodayActive);
//
//        // Используем лямбды для последовательности
//        btnToDay.setOnClickListener(v -> {
//            if (isTodayActive) return;
//            setActiveButton(true);
//        });
//
//        btnAWeek.setOnClickListener(v -> {
//            if (!isTodayActive) return;
//            setActiveButton(false);
//        });

    }

    // Вспомогательный метод для установки активной кнопки, смены изображений И ВЫСОТЫ (без изменений)
    private void setActiveButton(boolean selectToday) {
        int heightActivePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
        int heightInactivePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics());

        if (selectToday) {
            btnToDay.setImageResource(R.drawable.activetoday);
            ViewGroup.LayoutParams paramsToday = btnToDay.getLayoutParams();
            paramsToday.height = heightActivePx;
            btnToDay.setLayoutParams(paramsToday);

            btnAWeek.setImageResource(R.drawable.btnaweek);
            ViewGroup.LayoutParams paramsAWeek = btnAWeek.getLayoutParams();
            paramsAWeek.height = heightInactivePx;
            btnAWeek.setLayoutParams(paramsAWeek);

            isTodayActive = true;
        } else {
            btnToDay.setImageResource(R.drawable.btntoday);
            ViewGroup.LayoutParams paramsToday = btnToDay.getLayoutParams();
            paramsToday.height = heightInactivePx;
            btnToDay.setLayoutParams(paramsToday);

            btnAWeek.setImageResource(R.drawable.activeaweek);
            ViewGroup.LayoutParams paramsAWeek = btnAWeek.getLayoutParams();
            paramsAWeek.height = heightActivePx;
            btnAWeek.setLayoutParams(paramsAWeek);

            isTodayActive = false;
        }
    }
}
