package sample;

import com.sun.javafx.webkit.WebConsoleListener;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import netscape.javascript.JSObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static javafx.scene.control.cell.TextFieldTableCell.forTableColumn;


public class Controller implements Script.Callback {
@FXML WebView wb;
@FXML TextField webUrl; //"https://1xvzzc.host/ru/"
@FXML ProgressBar pg;
@FXML TextArea html_text;
@FXML TextArea log_text;
@FXML TextArea log2_text;
@FXML TextField urlText;
@FXML TextArea TENNISSCRIPT;
@FXML AnchorPane MAINPANE;
@FXML TabPane SCRIPTTAB;
@FXML TextField INFO;
@FXML GridPane LOGGRID;

@FXML ToggleButton ONSTARTBTN;
@FXML Rectangle SHAPE;
@FXML Label CARRETPOSITION;
@FXML ToggleButton DEBUG;
@FXML AnchorPane MOVEANCHOR;
@FXML TableView<ScriptString> SCRIPTTABLE;
@FXML TableColumn<ScriptString,String> SCRIPTCOL;

private WebEngine wE;
private Worker<Void> worker;
private Document doc;
private JSObject windowObject;
private String _mainURL;
private SimpleStringProperty _LogText = new SimpleStringProperty();
private SimpleStringProperty _ScriptText = new SimpleStringProperty();
private SimpleStringProperty _InfoText = new SimpleStringProperty();
private List<String> _FileScriptsNameList =  new ArrayList<>();
private Script _Sc ;
private List _stringBuffer = new ArrayList<>();
private Boolean _IsStarted =  false;
private int _CarretLine = 0;
private int _CarretPos = 0;
private int _CarretPositon = 0;
private int _scrollLine = 10;
private int _scrollCount = 0;

public class ScriptString {
    private String text;
    private int num;

    public ScriptString(){}

    public ScriptString(String text, Integer strNum) {
        this.text = "[" + strNum + "] :   " + text;
        this.num = strNum;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}

private ObservableList<ScriptString> data = FXCollections.observableArrayList();

    @FXML private void initialize() throws IOException {
        wE = wb.getEngine();
        worker = wE.getLoadWorker();
        if (!webUrl.getText().isEmpty())
            Refresh(webUrl.getText());
        _Sc = new Script(wE);
        Init();
        _Sc.set_LogText(_LogText);

        worker.stateProperty().addListener(new ChangeListener<Worker.State>() {

            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                Script.set_LoadState(newValue);

                if (newValue == Worker.State.SUCCEEDED) {
                    pg.progressProperty().unbind();
                    pg.setProgress(0);

                    try {
                        TransformerFactory transformerFactory = TransformerFactory
                                .newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        StringWriter stringWriter = new StringWriter();
                        transformer.transform(new DOMSource(wE.getDocument()),
                                new StreamResult(stringWriter));
                        String xml = stringWriter.getBuffer().toString();
                        html_text.setText(xml);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    wE.executeScript("let num = 0;");
                    wE.executeScript("let target = 0;");
                    wE.executeScript(
                            "window.addEventListener(\"mouseover\", function over(e) {" +
                            "target = e.target || e.srcElement;" +
                            "console.log('Tag = ' + target.tagName + '   classname = ' + target.className + '     id = ' + target.id + '  href = '+ target.href);" +
                            "});");
    //               doc = wE.getDocument();
    //               Element el = doc.getDocumentElement();


                }
                if(newValue == Worker.State.FAILED)//{
//                    log2_text.setStyle("-fx-text-fill: #1e88e5; -fx-font-size: 20px;");
                    _LogText.setValue(_LogText.getValue() + " Ошибка загрузки страницы \n");
                //} else //log2_text.setStyle("-fx-text-fill: #000000; -fx-font-size: 14px;");
            }
        });

//        log2_text.setStyle("-fx-text-fill: #1e88e5; -fx-font-size: 16px;");

        wE.setJavaScriptEnabled(true);
        WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
            if (!_IsStarted)   _LogText.setValue(_LogText.getValue() + " " +message + "\n");
        });

        _LogText.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                _scrollCount++;
                if(_scrollCount > _scrollLine)
                    log2_text.setScrollTop(_scrollLine);
                _scrollCount = 0;
            }
        });

    }

    @Override
    public void Refresh(String location){
        urlText.setText(location);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                pg.progressProperty().bind(worker.progressProperty());
                if (!location.isEmpty()) {
                    wE.load(webUrl.getText() + location);
                    urlText.setText(webUrl.getText() + location);
                }else Refresh();
            }
        });
    }

    @FXML public void Refresh(){
        pg.progressProperty().bind(worker.progressProperty());
        if (!webUrl.getText().isEmpty()) {
            _mainURL = webUrl.getText();
            urlText.setText(_mainURL);
            wE.load(_mainURL);
        }
    }

    @FXML public void OnKeyPresed(KeyEvent e){
        List<Element> EPath = new ArrayList<Element>();
        String EPathStr = "";
        String data_type = "";
        String _class = "";
        String href = "";
        String value = "";
        String tagName = "";
        String id  = "";
        KeyCode s = e.getCode();
        if (s == KeyCode.TAB) {
            Element parent;
            Element p = (Element) wE.executeScript("target");
            tagName = p.getTagName();
            data_type = p.getAttribute("data-type");
            _class = p.getAttribute("class");
            href = p.getAttribute("href");
            value = p.getAttribute("value");
            EPath.add(p);
            parent = (Element) p.getParentNode();
            EPath.add(parent);
            while (!parent.getTagName().equalsIgnoreCase("html")) {
                parent = (Element) parent.getParentNode();
                EPath.add(parent);
            }

            for(int i = EPath.size() -1; i >= 0; i--){
                EPathStr += EPath.get(i).getTagName() + " id = " +  EPath.get(i).getAttribute("id") + " ";
            }


            if (tagName.equalsIgnoreCase("SPAN")) {
            }
            if (tagName.equalsIgnoreCase("A")) {
            }
            if (tagName.equalsIgnoreCase("INPUT")) {
            }


            log2_text.setText("EPathStr =" + EPathStr + "\n" +"tagName =" + tagName + "\n" + "data-type = " + data_type + "\n" + "class = "
                    + _class + "\n" + "href = " + href + "\nValue  = " + value + "\n---------------------\n" + log2_text.getText());
        }

        e = null;
    }

    @FXML void OnStart(){
        data.clear();
        _IsStarted = ONSTARTBTN.isSelected();

        if(!_IsStarted) {
            _Sc.set_IsStop(true);
            ONSTARTBTN.setText("Start");
            return;
        }

        _Sc.set_Script(_stringBuffer);
        for(int i = 0; i < _stringBuffer.size(); i++)
            data.addAll(new ScriptString((String)_stringBuffer.get(i), i));

        _Sc.set_IsStop(false);
        if(!_Sc.Start()){ // Стартуем выполнение алгоритма
            ONSTARTBTN.setSelected(false);
            ONSTARTBTN.setText("Start");
        } else{
            ONSTARTBTN.setText("Stop");
            _LogText.setValue("");
        }

    }

    @FXML private void  LoadScript() throws FileNotFoundException {
        String _FileName= "";
        String _LoadText = "Не удалось загрузить файл";
        final FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("UWB files (*.uwb)", "*.uwb");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(MAINPANE.getScene().getWindow());
        if (file != null) {
            _FileName = file.getAbsolutePath();
            _InfoText.setValue(_FileName);
            _LoadText = ReadFile(file);
        }
         if(SCRIPTTAB.getSelectionModel().isSelected(0)) {
             _ScriptText.setValue(_LoadText);
             _FileScriptsNameList.add(0,_FileName);
         }
    }

    @FXML private void SaveScript(){
        final FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("UWB files (*.uwb)", "*.uwb");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(_InfoText.getValue());
        File file = fileChooser.showSaveDialog(MAINPANE.getScene().getWindow());

        if(file != null){
            SaveFile(_ScriptText.getValue(), file);
        }
    }

    @FXML private void OnScriptChanged(){

        int _ScriptIndex = SCRIPTTAB.getSelectionModel().getSelectedIndex();
        _stringBuffer.clear();
        if(_ScriptText.getValue() == null || _ScriptText.getValue().isEmpty()) return;

        int _StringLength = 0;
        Boolean _Flag = false;
        String[]  splittedStringArray = _ScriptText.getValue().split("\n");
        int c = 0;
        _CarretLine = splittedStringArray.length +1;
        _CarretPos = 0;

        for(String s : splittedStringArray){
            c++;
            _StringLength += s.length() +1;
            _stringBuffer.add(s + "\n");
            if (_CarretPositon  < _StringLength && !_Flag) { _CarretLine = c; _CarretPos = _CarretPositon - (_StringLength - s.length()) +1; _Flag = !_Flag;}
        }
        CARRETPOSITION.setText(_CarretLine + " : " + _CarretPos);

        _Sc.set_Script(_stringBuffer);


        if(DEBUG.isSelected()){
            if(!_Sc.TranspilatonCode()){
                _InfoText.set(_Sc.get_Error());
            } else _InfoText.set(_Sc.GetTranspilationString(_CarretLine));

        } else  _InfoText.set("");


    }

    private void SaveFile(String content, File file){
            try {
                FileWriter fileWriter = null;
                fileWriter = new FileWriter(file);
                fileWriter.write(content);
                fileWriter.close();
            } catch (IOException ex) {
            }

        }

    private String ReadFile(File file){
            _stringBuffer.clear();
            BufferedReader bufferedReader = null;
            String _result= "";
            try {

                bufferedReader = new BufferedReader(new FileReader(file));

                String text;
                while ((text = bufferedReader.readLine()) != null) {
                    _stringBuffer.add(text + "\n");
                }

            } catch (IOException ex) {
            }  finally {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                }
            }
            if(_stringBuffer.size() != 0){
                for (int i = 0; i < _stringBuffer.size(); i++){
                    _result += _stringBuffer.get(i);
                }
            }
            return _result;
        }

    private void Init(){
        _mainURL = webUrl.getText();

        log2_text.textProperty().bindBidirectional(_LogText);
        TENNISSCRIPT.textProperty().bindBidirectional(_ScriptText);
        INFO.textProperty().bindBidirectional(_InfoText);
        SCRIPTTAB.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if(newValue.getId().equalsIgnoreCase("TENNISTAB")) _ScriptText.setValue(TENNISSCRIPT.getText());
            }
        });
        _Sc.registerCallBack(this);

        TENNISSCRIPT.caretPositionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                _CarretPositon = (int) newValue;
            }
        });

        SCRIPTCOL.setCellValueFactory(new PropertyValueFactory<ScriptString, String>("text"));
        SCRIPTTABLE.setItems(data);

        _Sc.setSCRIPTTABLE(SCRIPTTABLE);
    }


    @Override
    public void OnStoped() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ONSTARTBTN.setSelected(false);
                ONSTARTBTN.setText("Start");
                _LogText.setValue(_LogText.getValue() + " " + _Sc.get_Error().substring(1,_Sc.get_Error().length() -1) + "\n");
            }
        });

    }
}
