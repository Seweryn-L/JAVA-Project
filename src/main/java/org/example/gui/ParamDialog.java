package org.example.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;

public class ParamDialog extends Dialog<ParamDialog.Result> {
    public static class Result {
        public final int newBeltCountMax;
        public final int newBeltWeightMax;
        public final int newTruckCapacity;

        public Result(int count, int weight, int truck) {
            this.newBeltCountMax = count;
            this.newBeltWeightMax = weight;
            this.newTruckCapacity = truck;
        }
    }

    public ParamDialog(int currentCount, int currentWeight, int currentTruck) {
        setTitle("Ustaw nowe parametry");
        setHeaderText("Wprowadź nowe parametry taśmy i ciężarówki");

        // Pola tekstowe
        TextField countField = new TextField(String.valueOf(currentCount));
        TextField weightField = new TextField(String.valueOf(currentWeight));
        TextField truckField = new TextField(String.valueOf(currentTruck));

        GridPane grid = new GridPane();
        grid.addRow(0, new Label("Max liczba cegieł na taśmie:"), countField);
        grid.addRow(1, new Label("Max masa taśmy:"), weightField);
        grid.addRow(2, new Label("Pojemność ciężarówki:"), truckField);

        getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new Result(
                        Integer.parseInt(countField.getText()),
                        Integer.parseInt(weightField.getText()),
                        Integer.parseInt(truckField.getText())
                );
            }
            return null;
        });
    }
}
