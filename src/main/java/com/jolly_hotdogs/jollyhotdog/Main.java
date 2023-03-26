package com.jolly_hotdogs.jollyhotdog;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {

    // UI components
    private TableView<Item> inventoryTable;
    private TableView<Sales> salesTable;
    private TableView<TotalSales> totalTable;
    private TableView<Item> newTable;
    private ComboBox<String> filterTypeComboBox, typeComboBox, itemSalesComboBox;
    private TextField nameField, quantityField, priceField, quantitySalesField, branchField, priceSalesField, typeSalesField;
    private Label statusLabel, searchLabel, salesLabel;

    // inventory data
    private final ObservableList<Item> inventoryData = FXCollections.observableArrayList();
    private final List<Item> inventoryList = new ArrayList<>();
    private final ObservableList<Sales> salesData = FXCollections.observableArrayList();
    private final List<Sales> salesList = new ArrayList<>();
    private final ObservableList<TotalSales> totalSalesData = FXCollections.observableArrayList(
            new TotalSales(0, 0, 0)
    );
    private String searchText = "";

    // file handling
    private File inventoryFile, SalesFile;

    @Override
    public void start(Stage stage) {
        // create UI components
        Label titleLabel = new Label("Jolly Hotdogs POS");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER);

        HBox filterBox = new HBox();
        filterBox.setSpacing(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        filterTypeComboBox = new ComboBox<>();
        filterTypeComboBox.setValue("All");
        filterTypeComboBox.setPromptText("Filter by Type");
        filterTypeComboBox.getItems().addAll("All", "Drink", "Sides", "Mini Dog", "Hotdog", "Hotdog Sandwich");
        filterTypeComboBox.setOnAction(event -> filterInventory());

        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name");
        searchField.setOnKeyReleased(event -> setSearchGlobal(searchField));

        Button removeButton = new Button("Remove Selected");
        removeButton.setOnAction(event -> removeItem());
        removeButton.setAlignment(Pos.CENTER_RIGHT);

        Button increaseButton = new Button("Add Selected");
        increaseButton.setOnAction(event -> increaseItem());
        increaseButton.setAlignment(Pos.CENTER_RIGHT);

        Button priceButton = new Button("Set Price Selected");
        priceButton.setOnAction(event -> changePrice());
        priceButton.setAlignment(Pos.CENTER_RIGHT);

        Button managerButton = new Button("Transaction Manager");
        managerButton.setOnAction(event -> transactionManager());
        managerButton.setAlignment(Pos.CENTER_RIGHT);

        searchLabel = new Label();
        searchLabel.setStyle("-fx-font-size: 12px;");
        searchLabel.setAlignment(Pos.CENTER_LEFT);

        filterBox.getChildren().addAll(filterTypeComboBox, searchField, removeButton, increaseButton, priceButton, managerButton);

        inventoryTable = new TableView<>();
        inventoryTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        inventoryTable.getColumns().forEach(column -> {
            column.setResizable(false);
            column.setMinWidth(50);
        });
        inventoryTable.setEditable(false);

        TableColumn<Item, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<Item, String> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().getQuantity())));
        quantityColumn.setCellFactory(column -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int quantity = Integer.parseInt(item);
                    if (quantity == 0) {
                        setStyle("-fx-background-color: red;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<Item, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getType().getDisplayName());
        });

        TableColumn<Item, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Double.toString(cellData.getValue().getPrice())));

        TableColumn<Item, String> lastTransactionColumn = new TableColumn<>("Last Transaction");
        lastTransactionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastTransaction().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        nameColumn.prefWidthProperty().bind(inventoryTable.widthProperty().multiply(0.3)); // 60% of the table width
        quantityColumn.prefWidthProperty().bind(inventoryTable.widthProperty().multiply(0.1)); // 40% of the table width
        typeColumn.prefWidthProperty().bind(inventoryTable.widthProperty().multiply(0.2)); // 60% of the table width
        priceColumn.prefWidthProperty().bind(inventoryTable.widthProperty().multiply(0.1)); // 40% of the table width
        lastTransactionColumn.prefWidthProperty().bind(inventoryTable.widthProperty().multiply(0.3)); // 40% of the table width

        inventoryTable.getColumns().addAll(Arrays.asList(nameColumn, quantityColumn, typeColumn, priceColumn, lastTransactionColumn));
        inventoryTable.setItems(inventoryData);

        HBox inputBox = new HBox();
        inputBox.setSpacing(10);
        inputBox.setAlignment(Pos.CENTER);

        nameField = new TextField();
        nameField.setPromptText("Name");

        quantityField = new TextField();
        quantityField.setPromptText("Quantity");

        typeComboBox = new ComboBox<>();
        typeComboBox.setPromptText("Type");
        typeComboBox.getItems().addAll("Drink", "Sides", "Mini Dog", "Hotdog", "Hotdog Sandwich");

        priceField = new TextField();
        priceField.setPromptText("Price");

        Button addButton = new Button("Add New Item");
        addButton.setOnAction(event -> addItem());

        inputBox.getChildren().addAll(nameField, quantityField, typeComboBox, priceField, addButton);

        Button importButton = new Button("Import");
        importButton.setOnAction(event -> importInventory(stage));

        Button exportButton = new Button("Export");
        exportButton.setOnAction(event -> exportInventory(stage));

        HBox fileBox = new HBox();
        fileBox.setSpacing(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.getChildren().addAll(importButton, exportButton);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px;");
        statusLabel.setAlignment(Pos.CENTER);

        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(titleLabel, filterBox, searchLabel, inventoryTable, inputBox, fileBox, statusLabel);

        Scene scene = new Scene(root, 800, 600);
        inventoryTable.prefHeightProperty().bind(scene.heightProperty());

        stage.setScene(scene);
        stage.setTitle("Inventory Manager");
        stage.show();
    }

    private void filterInventory() {
        filterInventoryByType();
    }

    // filters the inventory table based on the type
    private void filterInventoryByType() {
        String filterType = filterTypeComboBox.getValue() == null ? "All" : filterTypeComboBox.getValue();
        ObservableList<Item> filteredData = FXCollections.observableArrayList();

        if (filterType.equals("All") && searchText.isEmpty()) {
            filteredData.addAll(inventoryData);
        }
        else {
            filteredData.addAll(inventoryData.stream()
                    .filter(item -> (filterType.equals("All") || item.getType().toString().equals(filterType.replace(" ", "_").toUpperCase())) &&
                            (searchText.isEmpty() || item.getName().toLowerCase().contains(searchText.toLowerCase())))
                    .toList());

            if (!searchText.isEmpty()) {
                searchLabel.setText("Results for: " + searchText);
            } else {
                searchLabel.setText("");
            }
        }

        inventoryTable.setItems(filteredData);
    }

    private void changePrice() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Change Price");
            dialog.setHeaderText("Enter new price for " + selectedItem.getName());

            ButtonType confirmButton = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

            TextField priceField = new TextField(String.valueOf(selectedItem.getPrice()));
            priceField.setPromptText("Price");

            VBox vbox = new VBox();
            vbox.getChildren().addAll(new Label("New Price:"), priceField);
            dialog.getDialogPane().setContent(vbox);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButton) {
                    return priceField.getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(price -> {
                try {
                    double newPrice = Double.parseDouble(price);
                    selectedItem.setPrice(newPrice);
                    inventoryTable.refresh();
                    statusLabel.setText("Price for " + selectedItem.getName() + " changed to " + newPrice);
                    selectedItem.setLastTransaction(LocalDateTime.now());
                } catch (NumberFormatException e) {
                    statusLabel.setText("Error: Invalid input for price");
                }
            });
        } else {
            statusLabel.setText("Error: No item selected");
        }
    }

    private void setSearchGlobal(TextField searchField){
        searchText = searchField.getText();
        filterInventory();
    }

    // adds a new item to the inventory
    private void addItem() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Error: Name cannot be empty");
            return;
        }

        ItemType type = ItemType.valueOf(typeComboBox.getValue().replace(" ", "_").toUpperCase());
        if (type == null) {
            statusLabel.setText("Error: Type cannot be empty");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                statusLabel.setText("Error: Quantity cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Error: Invalid quantity");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) {
                statusLabel.setText("Error: Price cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Error: Invalid price");
            return;
        }

        Item newItem = new Item(name, quantity, type, price, LocalDateTime.now());
        inventoryList.add(newItem);
        inventoryData.add(newItem);
        statusLabel.setText("Added " + name);
        filterInventory();
    }

    // removes an item from the inventory
    private void removeItem() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            int selectedQuantity = selectedItem.getQuantity();
            TextInputDialog dialog = new TextInputDialog(String.valueOf(selectedQuantity));
            dialog.setTitle("Remove Item");
            dialog.setHeaderText("Remove " + selectedItem.getName() + " - Quantity: " + selectedQuantity);
            dialog.setContentText("Enter quantity to remove:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String input = result.get();
                try {
                    int quantityToRemove = Integer.parseInt(input);
                    if (quantityToRemove > 0 && quantityToRemove <= selectedQuantity) {
                        selectedItem.setQuantity(selectedQuantity - quantityToRemove);
                        selectedItem.setLastTransaction(LocalDateTime.now());
                        inventoryTable.refresh();
                        statusLabel.setText("Removed " + quantityToRemove + " " + selectedItem.getName() + " - Quantity: " + selectedQuantity);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Quantity");
                        alert.setHeaderText(null);
                        alert.setContentText("Please enter a valid quantity to remove.");
                        alert.showAndWait();
                    }
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Please enter a valid number.");
                    alert.showAndWait();
                }
            }
        }
    }

    private void increaseItem() {
        Item selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Increase Quantity");
            dialog.setHeaderText("Enter quantity to add:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                int quantityToAdd = Integer.parseInt(result.get());
                int newQuantity = selectedItem.getQuantity() + quantityToAdd;
                selectedItem.setLastTransaction(LocalDateTime.now());
                selectedItem.setQuantity(newQuantity);
                inventoryTable.refresh();
                statusLabel.setText("Increased quantity of " + selectedItem.getName() + " by " + quantityToAdd);
            }
        }
    }

    // imports items to the inventory from a CSV file
    private void importInventory(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Inventory File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        inventoryFile = selectedFile;
        if (selectedFile != null) {
            try (Scanner scanner = new Scanner(selectedFile, StandardCharsets.UTF_8)) {
                List<Item> importedItems = new ArrayList<>();
                scanner.nextLine(); // skip header
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] parts = line.split(",");
                    String name = parts[0].trim();
                    int quantity = Integer.parseInt(parts[1].trim());
                    ItemType type = ItemType.valueOf(parts[2].trim());
                    double price = Double.parseDouble(parts[3].trim());
                    LocalDateTime lastTransaction = LocalDateTime.parse(parts[4].trim());

                    Item newItem = new Item(name, quantity, type, price, lastTransaction);
                    importedItems.add(newItem);
                }
                inventoryList.addAll(importedItems);
                inventoryData.addAll(importedItems);
                statusLabel.setText("Imported " + importedItems.size() + " items from " + selectedFile.getName());
            } catch (IOException e) {
                statusLabel.setText("Error importing items from " + selectedFile.getName());
            }
        }
        filterInventory();
    }

    // exports the inventory to a CSV file
    private void exportInventory(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Inventory File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(selectedFile), true, StandardCharsets.UTF_8)) {
                writer.println("Name,Quantity,Type,Price,Last Transaction");
                for (Item item : inventoryList) {
                    String name = item.getName();
                    ItemType type = item.getType();
                    LocalDateTime lastTransaction = item.getLastTransaction();
                    writer.println(name + "," + item.getQuantity() + "," + type + "," + item.getPrice() + "," + lastTransaction);
                }
                statusLabel.setText("Inventory exported to " + selectedFile.getName());
            } catch (IOException e) {
                statusLabel.setText("Error exporting inventory to " + selectedFile.getName());
            }
        }
    }

    private void transactionManager(){

        Stage stage = new Stage();
        stage.setTitle("Transaction Manager");

        Label titleLabel = new Label("Sales Report");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER);

        Label branchLabel = new Label("Branch: ");
        branchLabel.setStyle("-fx-font-size: 20px;");

        branchField  = new TextField("FEU TECH");
        branchField.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label dateLabel = new Label("Date: " + LocalDate.now().toString());
        dateLabel.setStyle("-fx-font-size: 18px");

        VBox branchAndDateBox = new VBox(10, branchLabel, branchField, dateLabel);

        salesTable = new TableView<>();
        salesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        salesTable.getColumns().forEach(column -> {
            column.setResizable(false);
            column.setMinWidth(50);
        });
        salesTable.setEditable(false);

        TableColumn<Sales, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<Sales, String> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Integer.toString(cellData.getValue().getQuantity())));

        TableColumn<Sales, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> {
            Sales Sales = cellData.getValue();
            return new SimpleStringProperty(cellData.getValue().getType().getDisplayName());
        });

        TableColumn<Sales, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(Double.toString(cellData.getValue().getPrice())));

        TableColumn<Sales, String> grossColumn = new TableColumn<>("Gross Income");
        grossColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getPrice() * cellData.getValue().getQuantity())));

        TableColumn<Sales, String> taxColumn = new TableColumn<>("Tax 12%");
        taxColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getPrice() * cellData.getValue().getQuantity() * 0.12)));

        TableColumn<Sales, String> afterTaxColumn = new TableColumn<>("After-Tax Income");
        afterTaxColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getPrice() * cellData.getValue().getQuantity() - cellData.getValue().getPrice() * cellData.getValue().getQuantity() * 0.12)));

        nameColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.3)); // 60% of the table width
        quantityColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.05)); // 40% of the table width
        typeColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.15)); // 60% of the table width
        priceColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.5/4)); // 40% of the table width
        grossColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.5/4)); // 40% of the table width
        taxColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.5/4)); // 40% of the table width
        afterTaxColumn.prefWidthProperty().bind(salesTable.widthProperty().multiply(0.5/4)); // 40% of the table width

        salesTable.getColumns().addAll(Arrays.asList(nameColumn, quantityColumn, typeColumn, priceColumn, grossColumn, taxColumn, afterTaxColumn));
        salesTable.setItems(salesData);

        totalTable = new TableView<>();
        totalTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        totalTable.getColumns().forEach(column -> {
            column.setResizable(false);
            column.setMinWidth(50);
        });
        totalTable.setEditable(false);
        totalTable.setFixedCellSize(30);
        totalTable.setMaxHeight(80);
        totalTable.setMinHeight(80);

        TableColumn<TotalSales, String> totalTable1 = new TableColumn<>("Total Gross Income");
        totalTable1.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getGross())));

        TableColumn<TotalSales, String> totalTable2 = new TableColumn<>("Total Tax Deduction");
        totalTable2.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getTax())));

        TableColumn<TotalSales, String> totalTable3 = new TableColumn<>("Total After Tax Income");
        totalTable3.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getAfterTax())));

        totalTable1.prefWidthProperty().bind(totalTable.widthProperty().multiply(0.33)); // 60% of the table width
        totalTable2.prefWidthProperty().bind(totalTable.widthProperty().multiply(0.33)); // 40% of the table width
        totalTable3.prefWidthProperty().bind(totalTable.widthProperty().multiply(0.34)); // 60% of the table width

        calculateTotal();

        totalTable.getColumns().addAll(Arrays.asList(totalTable1, totalTable2, totalTable3));
        totalTable.setItems(totalSalesData);

        HBox inputBox = new HBox();
        inputBox.setSpacing(10);
        inputBox.setAlignment(Pos.CENTER);

        quantitySalesField = new TextField();
        quantitySalesField.setPromptText("Quantity");
        quantitySalesField.setEditable(false);

        typeSalesField = new TextField();
        typeSalesField.setPromptText("Type");
        typeSalesField.setEditable(false);

        priceSalesField = new TextField();
        priceSalesField.setPromptText("Price");
        priceSalesField.setEditable(false);

        itemSalesComboBox = new ComboBox<>();
        itemSalesComboBox.setPromptText("Item");
        itemSalesComboBox.setOnAction(event -> {
            String selectedItemName = itemSalesComboBox.getSelectionModel().getSelectedItem();
            Item selectedItem = inventoryData.stream()
                    .filter(item -> item.getName().equals(selectedItemName))
                    .findFirst().orElse(null);
            if (selectedItem != null) {
                quantitySalesField.setText(Integer.toString(selectedItem.getQuantity()));
                typeSalesField.setText(selectedItem.getType().getDisplayName());
                priceSalesField.setText(Double.toString(selectedItem.getPrice()));
            }
        });

        for (Item entry : inventoryList) {
            itemSalesComboBox.getItems().add(entry.getName());
        }

        Button addButton = new Button("Add New Sale");
        addButton.setOnAction(event -> addSale(stage));

        inputBox.getChildren().addAll(itemSalesComboBox, quantitySalesField, typeSalesField, priceSalesField, addButton);

        salesLabel = new Label();
        salesLabel.setStyle("-fx-font-size: 12px;");
        salesLabel.setAlignment(Pos.CENTER);

        Button importButton = new Button("Import");
        importButton.setOnAction(event -> importSales(stage));

        Button exportButton = new Button("Export");
        exportButton.setOnAction(event -> exportSales(stage));

        HBox fileBox = new HBox();
        fileBox.setSpacing(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.getChildren().addAll(importButton, exportButton);

        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.TOP_CENTER);
        Scene scene = new Scene(root, 800, 600);
        root.getChildren().addAll(titleLabel, branchAndDateBox, searchLabel, salesTable, totalTable, inputBox, fileBox, salesLabel);

        salesTable.prefHeightProperty().bind(scene.heightProperty());

        stage.setScene(scene);
        stage.show();
    }

    private void calculateTotal() {
        salesTable.setItems(salesData);
        if(totalSalesData.isEmpty()) {
            totalSalesData.add(new TotalSales(calculateTotalGross(), calculateTotalTax(), calculateTotalAfterTax()));
        } else {
            TotalSales totalSales = totalSalesData.get(0);
            totalSales.setGross(calculateTotalGross());
            totalSales.setTax(calculateTotalTax());
            totalSales.setAfterTax(calculateTotalAfterTax());
        }
        totalTable.setItems(totalSalesData);
    }

    // Calculate the total gross income
    private double calculateTotalGross() {
        double totalGross = 0;
        for (Sales sale : salesTable.getItems()) {
            totalGross += sale.getPrice() * sale.getQuantity();
        }
        return totalGross;
    }

    // Calculate the total tax
    private double calculateTotalTax() {
        double totalTax = 0;
        for (Sales sale : salesTable.getItems()) {
            totalTax += sale.getPrice() * sale.getQuantity() * 0.12;
        }
        return totalTax;
    }

    // Calculate the total after-tax income
    private double calculateTotalAfterTax() {
        double totalAfterTax = 0;
        for (Sales sale : salesTable.getItems()) {
            totalAfterTax += sale.getPrice() * sale.getQuantity() - sale.getPrice() * sale.getQuantity() * 0.12;
        }
        return totalAfterTax;
    }

    private void addSale(Stage stage) {

        String branch = branchField.getText().trim();
        if (branch.isEmpty()) {
            salesLabel.setText("Error: Branch cannot be empty");
            return;
        }

        String name = itemSalesComboBox.getValue().trim();
        if (name.isEmpty()) {
            salesLabel.setText("Error: Name cannot be empty");
            return;
        }

        ItemType type = ItemType.valueOf(typeSalesField.getText().replace(" ", "_").toUpperCase());

        int quantity;
        try {
            quantity = Integer.parseInt(quantitySalesField.getText().trim());
            if (quantity < 1) {
                salesLabel.setText("Error: Quantity must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            salesLabel.setText("Error: Invalid quantity");
            return;
        }

        double price = Double.parseDouble(priceSalesField.getText().trim());

        int quantityToRemove = 0;
        Item selectedItem = inventoryData.stream()
                .filter(item -> item.getName().equals(name))
                .findFirst().orElse(null);
        if (selectedItem != null) {
            int selectedQuantity = selectedItem.getQuantity();
            TextInputDialog dialog = new TextInputDialog(String.valueOf(selectedQuantity));
            dialog.setTitle("Finalise Sale");
            dialog.setHeaderText("Item: " + selectedItem.getName() + " - Quantity: " + selectedQuantity);
            dialog.setContentText("Enter quantity of Sale:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String input = result.get();
                try {
                    quantityToRemove = Integer.parseInt(input);
                    if (quantityToRemove > 0 && quantityToRemove <= selectedQuantity) {
                        selectedItem.setQuantity(selectedQuantity - quantityToRemove);
                        selectedItem.setLastTransaction(LocalDateTime.now());
                        inventoryTable.refresh();
                        salesLabel.setText("Removed " + quantityToRemove + " " + selectedItem.getName() + " - Quantity: " + selectedQuantity);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Quantity");
                        alert.setHeaderText(null);
                        alert.setContentText("Please enter a valid quantity to remove.");
                        alert.showAndWait();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Please enter a valid number.");
                    alert.showAndWait();
                    return;
                }
            }
        }

        Sales newSale = new Sales(branch, name, quantityToRemove, type, price, LocalDateTime.now());
        salesList.add(newSale);
        salesData.add(newSale);
        salesLabel.setText("Added " + name);
        filterInventory();

        stage.close();
        transactionManager();
    }

    // imports items to the inventory from a CSV file
    private void importSales(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Sales File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        SalesFile = selectedFile;
        if (selectedFile != null) {
            try (Scanner scanner = new Scanner(selectedFile, StandardCharsets.UTF_8)) {
                List<Sales> importedSales = new ArrayList<>();
                scanner.nextLine(); // skip header
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] parts = line.split(",");

                    String branch = parts[0].trim();
                    LocalDateTime lastTransaction = LocalDateTime.parse(parts[1].trim());
                    String name = parts[2].trim();
                    int quantity = Integer.parseInt(parts[3].trim());
                    ItemType type = ItemType.valueOf(parts[4].trim());
                    double price = Double.parseDouble(parts[5].trim());

                    Sales newSale = new Sales(branch, name, quantity, type, price, lastTransaction);
                    importedSales.add(newSale);
                }
                salesList.addAll(importedSales);
                salesData.addAll(importedSales);
                salesLabel.setText("Imported " + importedSales.size() + " items from " + selectedFile.getName());
            } catch (IOException e) {
                salesLabel.setText("Error importing items from " + selectedFile.getName());
            }
            salesTable.setItems(salesData);
        }

        stage.close();
        transactionManager();
    }

    // exports the inventory to a CSV file
    private void exportSales(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sales File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(selectedFile), true, StandardCharsets.UTF_8)) {
                writer.println("Branch,Date,Item,Quantity,Type,Price");
                for (Sales sale : salesList) {
                    String branch = sale.getBranch();
                    String name = sale.getName();
                    LocalDateTime lastTransaction = sale.getTransactionDate();
                    int quantity = sale.getQuantity();
                    double price = sale.getPrice();
                    ItemType type = sale.getType();
                    writer.println(branch + "," + lastTransaction + "," + name + "," + quantity + "," + type + "," + price);
                }
                salesLabel.setText("Sales exported to " + selectedFile.getName());
            } catch (IOException e) {
                salesLabel.setText("Error exporting Sales to " + selectedFile.getName());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}