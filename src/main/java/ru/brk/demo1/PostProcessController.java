package ru.brk.demo1;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;
import java.io.*;


public class PostProcessController {

    @FXML
    private Button backButton;
    @FXML
    private TableView<ResultRow> resultsTable;

    // ---------------------------
    // КЛАСС ДЛЯ ХРАНЕНИЯ ДАННЫХ О СТЕРЖНЕ
    // ---------------------------
    public static class Rod {
        double L;      // длина стержня
        double A;      // площадь поперечного сечения
        double E;      // модуль Юнга
        double sigma;  // допустимое напряжение (или что-то ещё)

        // typeLoad - например, параметр, который показывает, есть ли распределённая нагрузка и т.д.
        // Но в примере я отделил это от стержня. Если нужно, можно хранить здесь.
        public Rod(double L, double A, double E, double sigma) {
            this.L = L;
            this.A = A;
            this.E = E;
            this.sigma = sigma;
        }
    }

    // ---------------------------
    // КЛАСС ДЛЯ ХРАНЕНИЯ ДАННЫХ О НАГРУЗКЕ
    // ---------------------------
    public static class Load {
        // indexNode - номер узла (в нумерации от 0)
        int indexNode;
        // value - величина нагрузки (сила, например, F)
        double value;
        // typeLoad - 1 = сосредоточенная, 2 = распределённая (пример)
        int typeLoad;

        public Load(int indexNode, double value, int typeLoad) {
            this.indexNode = indexNode;
            this.value = value;
            this.typeLoad = typeLoad;
        }
    }

    // ---------------------------
    // ДАННЫЕ ДЛЯ РЕЗУЛЬТАТОВ В ТАБЛИЦУ
    // ---------------------------
    public static class ResultRow {
        int rodIndex;
        double dispStart;    // перемещение в начале стержня
        double dispEnd;      // перемещение в конце стержня
        double forceStart;   // продольная сила в начале
        double forceEnd;     // продольная сила в конце
        double stressStart;  // напряжение в начале
        double stressEnd;    // напряжение в конце
        double sigmaAllow;   // допустимое напряжение
        boolean checkStart;
        boolean checkEnd;

        public ResultRow(int rodIndex,
                         double dispStart, double dispEnd,
                         double forceStart, double forceEnd,
                         double stressStart, double stressEnd,
                         double sigmaAllow,
                         boolean checkStart, boolean checkEnd) {
            this.rodIndex = rodIndex;
            this.dispStart = dispStart;
            this.dispEnd = dispEnd;
            this.forceStart = forceStart;
            this.forceEnd = forceEnd;
            this.stressStart = stressStart;
            this.stressEnd = stressEnd;
            this.sigmaAllow = sigmaAllow;
            this.checkStart = checkStart;
            this.checkEnd = checkEnd;
        }
    }

    // ---------------------------
    // СПИСКИ СТЕРЖНЕЙ И НАГРУЗОК
    // ---------------------------
    private List<Rod> rods = new ArrayList<>();
    private List<Load> loads = new ArrayList<>();

    private boolean supportStart = true;  // жёсткая заделка в начале
    private boolean supportEnd = true;    // жёсткая заделка в конце

    private double[] displacements;
    private double reactionStart;
    private double reactionEnd;

    @FXML
    void initialize() {
        backButton.setOnAction(actionEvent -> {
            SceneSwitcher.openAnotherScene(backButton, "hello-view.fxml");
        });

        try {
            loadData("src/main/resources/test.cn", "src/main/resources/test.ld");
            calculateResults();
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // 1. ЗАГРУЗКА ДАННЫХ ИЗ ФАЙЛОВ
    // ----------------------------------------------------------
    private void loadData(String cnFile, String ldFile) throws IOException {

        // ---------------------------------
        // ЧТЕНИЕ cn (СТЕРЖНИ)
        // Формат (пример):
        // A  L  E  sigma
        // ---------------------------------
        try (BufferedReader br = new BufferedReader(new FileReader(cnFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                double A = Double.parseDouble(parts[0]);
                double L = Double.parseDouble(parts[1]);
                double E = 1.0;    // по умолчанию
                double sigma = 10.0; // по умолчанию

                if (parts.length >= 3) {
                    // третий параметр — "тип" стержня или ещё что-то
                    // Вы говорили, что "3 параметр ... от 1 до 2", но давайте лучше введём E
                    // или просто положим "тип" в переменную, если нужно
                    // В примере я трактую как E
                    E = Double.parseDouble(parts[2]);
                }
                if (parts.length >= 4) {
                    sigma = Double.parseDouble(parts[3]);
                }
                // Добавляем в список
                rods.add(new Rod(L, A, E, sigma));
            }
        }

        // ---------------------------------
        // ЧТЕНИЕ ld (НАГРУЗКИ)
        // Формат (пример):
        // nodeIndex  value  loadType
        // nodeIndex  value  loadType
        // ---------------------------------
        try (BufferedReader br = new BufferedReader(new FileReader(ldFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                // Допустим, parts[0] = узел (1-based), parts[1] = величина силы, parts[2] = тип нагрузки
                int nodeIndex = (int)(Double.parseDouble(parts[0]) - 1);
                double power  = Double.parseDouble(parts[1]);
                int typeLoad  = 1;
                if (parts.length >= 3) {
                    typeLoad = (int)Double.parseDouble(parts[2]);
                }

                loads.add(new Load(nodeIndex, power, typeLoad));
            }
        }
    }

    // ----------------------------------------------------------
    // 2. СБОРКА ГЛОБАЛЬНОЙ МАТРИЦЫ ЖЁСТКОСТИ
    // ----------------------------------------------------------
    private double[][] generateStiffnessMatrix() {
        // Если у нас N стержней, то узлов будет N+1
        int size = rods.size() + 1;
        double[][] K = new double[size][size];

        for (int i = 0; i < rods.size(); i++) {
            Rod rod = rods.get(i);
            double kLocal = rod.E * rod.A / rod.L; // жёсткость стержня

            K[i][i]       += kLocal;
            K[i][i + 1]   -= kLocal;
            K[i + 1][i]   -= kLocal;
            K[i + 1][i + 1] += kLocal;
        }

        if (supportStart) {
            // Узел 0
            for (int j = 0; j < size; j++) {
                K[0][j] = 0.0;
                K[j][0] = 0.0;
            }
            K[0][0] = 1.0;
        }
        if (supportEnd) {
            for (int j = 0; j < size; j++) {
                K[size - 1][j] = 0.0;
                K[j][size - 1] = 0.0;
            }
            K[size - 1][size - 1] = 1.0;
        }

        return K;
    }

    // ----------------------------------------------------------
    // 3. ФОРМИРОВАНИЕ ГЛОБАЛЬНОГО ВЕКТОРА СИЛ
    // ----------------------------------------------------------
    private double[] generateForceVector() {
        int size = rods.size() + 1;
        double[] F = new double[size];

        // 3.1. Сосредоточенные силы (typeLoad = 1)
        for (Load load : loads) {
            if (load.typeLoad == 1) {
                int iNode = load.indexNode;
                F[iNode] += load.value;
            }
        }

        // 3.2. Распределённые нагрузки (typeLoad = 2).
        //     Предположим, что каждая распределённая нагрузка F(value) действует на соответствующий "стержень"
        //     (или узел?). Тут нужно чётко понимать вашу логику.
        //     Например, если load.indexNode = i, то стержень i-ый.
        //     Экв. узловые силы: q * L / 2 на каждый конец (или что-то похожее).
        //     Допустим, load.value = q (Н/м).
        //     Считаем, что эта нагрузка "лежит" на i-том стержне.
        //     Тогда:
        for (int i = 0; i < rods.size(); i++) {
            Rod rod = rods.get(i);
            // Проверим, есть ли load, привязанная к этому стержню (indexNode = i).
            // В реальности может быть несколько таких нагрузок.
            double q = 0.0; // по умолчанию
            for (Load load : loads) {
                if (load.typeLoad == 2 && load.indexNode == i) {
                    q += load.value;
                }
            }
            if (Math.abs(q) > 1e-14) {
                // эквивалентная сосредоточенная сила на каждый узел
                double eq = q * rod.L / 2.0;
                // F[i], F[i+1]
                F[i]     += eq;
                F[i + 1] += eq;
            }
        }

        // Учёт заделок (перемещение = 0 -> F[...] = 0)
        if (supportStart) {
            F[0] = 0.0;
        }
        if (supportEnd) {
            F[F.length - 1] = 0.0;
        }

        return F;
    }

    // ----------------------------------------------------------
    // 4. РЕШЕНИЕ СИСТЕМЫ МЕТОДОМ ГАУССА (ИЛИ ЛЮБЫМ ДРУГИМ)
    // ----------------------------------------------------------
    private double[] solveDisplacements() {
        double[][] K = generateStiffnessMatrix();
        double[] F = generateForceVector();
        int size = K.length;
        double[] disp = new double[size];

        // Прямой ход
        for (int i = 0; i < size; i++) {
            double pivot = K[i][i];
            if (Math.abs(pivot) < 1e-14) {
                // ищем перестановку
                for (int r = i + 1; r < size; r++) {
                    if (Math.abs(K[r][i]) > 1e-14) {
                        // swap строк i и r
                        double[] tmpRow = K[i];
                        K[i] = K[r];
                        K[r] = tmpRow;

                        double tmpVal = F[i];
                        F[i] = F[r];
                        F[r] = tmpVal;

                        pivot = K[i][i];
                        break;
                    }
                }
            }
            // обычная фиксация pivot'а
            for (int j = i + 1; j < size; j++) {
                double ratio = K[j][i] / pivot;
                for (int k = i; k < size; k++) {
                    K[j][k] -= ratio * K[i][k];
                }
                F[j] -= ratio * F[i];
            }
        }

        // Обратный ход
        for (int i = size - 1; i >= 0; i--) {
            double sum = F[i];
            for (int j = i + 1; j < size; j++) {
                sum -= K[i][j] * disp[j];
            }
            disp[i] = sum / K[i][i];
        }

        return disp;
    }

    // ----------------------------------------------------------
    // 5. ОСНОВНОЙ МЕТОД: ВЫЧИСЛЕНИЕ, ВЫВОД
    // ----------------------------------------------------------
    private void calculateResults() {
        displacements = solveDisplacements();

        // Посчитаем реакции (если интересно) – это сила в заделанных узлах:
        // R0 = \sum_j K(0,j)*u[j]  (но у нас после обнуления строки там K(0,0)=1, ...)
        // Можно подсчитать на старой матрице, но здесь для наглядности сделаем вручную:
        reactionStart = 0.0;
        reactionEnd   = 0.0;
        // Возьмём первый стержень:
        if (rods.size() > 0) {
            // Сила = E*A/L * (u2 - u1)
            double n0 = rods.get(0).E * rods.get(0).A / rods.get(0).L
                    * (displacements[1] - displacements[0]);
            reactionStart = -n0; // реакция в узле 0 (с учётом знака)
        }
        // Возьмём последний стержень:
        if (rods.size() > 0) {
            int last = rods.size() - 1;
            // там узлы (last) и (last+1)
            double nEnd = rods.get(last).E * rods.get(last).A / rods.get(last).L
                    * (displacements[last + 1] - displacements[last]);
            reactionEnd = nEnd; // реакция в последнем узле (size-1)
        }

        for (int i = 0; i < rods.size(); i++) {
            Rod rod = rods.get(i);

            double uStart = displacements[i];
            double uEnd   = displacements[i + 1];

            double NxStart = rod.E * rod.A / rod.L * (uEnd - uStart);
            double NxEnd   = NxStart;  //
            // Напряжения
            double sigmaStart = NxStart / rod.A;
            double sigmaEnd   = NxEnd   / rod.A;

            boolean checkStart = (Math.abs(sigmaStart) <= rod.sigma);
            boolean checkEnd   = (Math.abs(sigmaEnd)   <= rod.sigma);

            System.out.printf("Стержень %d:\n", i + 1);
            System.out.printf("  u[%d] = %.4f, u[%d] = %.4f\n", i, uStart, i + 1, uEnd);
            System.out.printf("  NxStart = %.4f, NxEnd = %.4f\n", NxStart, NxEnd);
            System.out.printf("  sigmaStart = %.4f, sigmaEnd = %.4f\n", sigmaStart, sigmaEnd);
            System.out.printf("  Допускаемое sigma = %.4f\n", rod.sigma);
            System.out.printf("  Проверка: %s / %s\n\n", checkStart?"OK":"FAIL", checkEnd?"OK":"FAIL");

            if (resultsTable != null) {
                resultsTable.getItems().add(
                        new ResultRow(i+1,
                                uStart, uEnd,
                                NxStart, NxEnd,
                                sigmaStart, sigmaEnd,
                                rod.sigma, checkStart, checkEnd
                        )
                );
            }
        }

        displayResults();
    }

    // ----------------------------------------------------------
    // 6. ВЫВОД РЕЗУЛЬТАТОВ (на экран)
    // ----------------------------------------------------------
    private void displayResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("Результаты расчёта:\n");

        for (int i = 0; i < rods.size(); i++) {
            Rod rod = rods.get(i);
            double uS = displacements[i];
            double uE = displacements[i + 1];
            double Nx = rod.E * rod.A / rod.L * (uE - uS);
            double sigmaCalc = Nx / rod.A;
            boolean check = (Math.abs(sigmaCalc) <= rod.sigma);

            sb.append(String.format("Стержень %d:\n", i+1));
            sb.append(String.format("  Перемещения узлов: u[%d] = %.4f, u[%d] = %.4f\n", i, uS, i+1, uE));
            sb.append(String.format("  Сила Nx = %.4f\n", Nx));
            sb.append(String.format("  Напряжение sigma = %.4f (допустимое: %.4f)\n", sigmaCalc, rod.sigma));
            sb.append("  Проверка: ").append(check ? "OK" : "FAIL").append("\n\n");
        }

        sb.append(String.format("Реакция в начале (узел 0): %.4f\n", reactionStart));
        sb.append(String.format("Реакция в конце (узел %d): %.4f\n", rods.size(), reactionEnd));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Результаты расчёта");
        alert.setHeaderText("Результаты:");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }
}