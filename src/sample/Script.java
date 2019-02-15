package sample;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class   Script {

    interface Callback{
        void OnStoped();
        void Refresh(String location);
    }
    Callback callback;

    Rectangle SHAPE;
    private WebEngine _wE;
    TableView SCRIPTTABLE;
    private List<String> _Script = new ArrayList<>();
    private List<String> _TransScript = new ArrayList<>();
    private List<String> _Words = new ArrayList<>();

    private int _Brackets = 0;
    private int _Commas = 0;
    private List<String>  _Error = new ArrayList<>();
    private TextArea _ScriptArea;
    private SimpleStringProperty _LogText = new SimpleStringProperty();

    static public Boolean _IsStop = true;
    static public Worker.State _LoadState = Worker.State.RUNNING ;
    private Map<String,String> CommandList = new HashMap<>();
    private int _ValCount = 0;
    private int _ScrolCounter = 0;
    private int _ScrollString;

    int  _IsIF = -1;
    boolean _isError = false;

    public void set_LogText(SimpleStringProperty _LogText) {
        this._LogText = _LogText;
    }

    class Cycle {
        public  int _Counter;
        public  int _FirstString;
        public  int _LastString;
        public  String _Item;

        public Cycle(int _Counter, int _FirstString, int _LastString, String _Item) {
            this._Counter = _Counter;
            this._FirstString = _FirstString;
            this._LastString = _LastString;
            this._Item = _Item;
        }
    }

    private Stack<Cycle> _CycleStack = new Stack<>();


    public Script(WebEngine wE) {

        this._wE = wE;
        InitComanMap();
    }

    public void set_ScriptArea(TextArea _ScriptArea) {
        this._ScriptArea = _ScriptArea;
    }

    public void setSHAPE(Rectangle SHAPE) {this.SHAPE = SHAPE;}

    public boolean Start(){
        _Error.clear();
        if(CheckError()) return false;
        _LoadState = Worker.State.RUNNING;
        _IsStop = false;
        _ScrolCounter = 0;
        _ScrollString = 5;

        TranspilatonCode();

        return StartScript();
    }

    public void set_Script(List<String> _Script) {
        this._Script = _Script;
    }


    public String get_Error() {return _Error.toString();}

    private boolean CheckError(){
        if(_wE == null) _Error.add("Не указан web engine");
        if(_Script.size() == 0) _Error.add("Не загружен код алгоритма");
        return !_Error.isEmpty();
        }

    private void InitComanMap(){
    CommandList.put("Загрузить","Загрузить");
    CommandList.put("ПолучитьЭлементПоКлассу","document.getElementsByClassName");
    CommandList.put("ПолучитьЭлементПоИд","document.getElementById");
    CommandList.put("Установить","var");
    CommandList.put("ДляВсех","ДляВсех");
    CommandList.put("Нажать","click()");
    CommandList.put("Ждать","Ждать");
    CommandList.put("Потомок","children");
    CommandList.put("Текст","innerText");
    CommandList.put("Если","if");
    CommandList.put("Иначе","else");
    CommandList.put("УстановитьАтрибут","setAttribute");
    CommandList.put("Стиль","'style'");
    CommandList.put("Значение","value");
    CommandList.put("Повторить","Повторить");
    CommandList.put("Родитель","parentNode");
    CommandList.put("УдалитьРебенка","removeChild");
    CommandList.put("Остановить","Остановить");
    CommandList.put("Покинуть","Покинуть");
    CommandList.put("Обновить","window.location.reload()");







    CommandList.put("=","=");
    CommandList.put("+","+");
    CommandList.put("-","-");
    CommandList.put("*","*");
    CommandList.put("/","/");
    CommandList.put("(","(");
    CommandList.put(")",")");
    CommandList.put("\"","\"");
    CommandList.put(";",";");
    CommandList.put("{","{");
    CommandList.put("}","}");
    CommandList.put(".",".");
    CommandList.put("[","[");
    CommandList.put("]","]");
    CommandList.put("'","'");
    CommandList.put("&","&");





     }

    public void set_IsStop(Boolean _IsStop) {this._IsStop = _IsStop;}

    public boolean TranspilatonCode(){
        CommandList.clear();
        InitComanMap();
        _TransScript.clear();
        _Error.clear();
        _ValCount = 0;
        _Brackets = 0;
        InitComanMap();

        for (int i = 0; i < _Script.size(); i++){
            _Words.clear();

            String _newLine = _Script.get(i).substring(0,_Script.get(i).length() - 1).replace("\n", "").trim();
            String _TranString = "";

//            if(_newLine.isEmpty()){
//                continue;
//            }

            String _Command = "";
            if(!_newLine.endsWith(";") && !_newLine.endsWith("{")) _newLine += ";";
            for(Character s : _newLine.toCharArray()){// Разделить все на отдельные слова

                if( s == '{' || s == '(' || s == '[') _Brackets ++;
                if( s == '}' || s == ')' || s == ']') _Brackets --;
                if (s == '\"') _Commas ++;

                if(s == ' ' || s == '=' || s == '+' || s == '-' || s == '*' || s == '/' || s == '(' || s == ')' || s == '"'
                        || s == ';' || s == '{' || s == '}' || s == '.' || s == '[' || s == ']' || s == ',' || s == '\'' ){
                    if (!_Command.trim().isEmpty())_Words.add(_Command);
                    _Words.add(s.toString());
                    _Command = "";
                } else _Command += s;
            }

            boolean _isLet= false;
            for(String _cw : _Words){// ищем и сохраняем переменные
                if(_cw.trim().isEmpty()) continue;
                if(_isLet) {
                    if(CommandList.containsKey(_cw)){
                        _Error.add("Повторное обьявление переменной '" + _cw + "' в строке " + (i + 1));
                        _isLet = false;
                        continue;
                    }
                    CommandList.put(_cw,"val_" + _ValCount++);
                    _isLet = false;
                }
                if(_cw.equalsIgnoreCase("установить")) _isLet = true;
            }

            Boolean _isString = false;
            Boolean _isParam = false;
            for(String _cw : _Words) {// делаем замену команд и переменных
                if (_cw.equals("\"") || _cw.equals("\'")) _isString = !_isString;
                if (_cw.equals("[") || _cw.equals("(")) _isParam = true;
                if (_cw.equals("]") || _cw.equals(")")) _isParam = false;
                Boolean _isNumder = CheckNumber(_cw);

                if (CommandList.containsKey(_cw)) _TranString += CommandList.get(_cw);
                else {
                    _TranString += _cw;
                    if(!_cw.equals(" ")  && !_isString && !_isParam && !_isNumder)
                        _Error.add("Не извесная команда " + _cw + " в строке "+ (i + 1));
                }
            }
            if (_isString) _Error.add("Не хватает \"" + " в строке "+ (i + 1));

            _TransScript.add(_TranString);
            }

            if (_Brackets != 0 || _Commas%2 != 0) _Error.add(" Синтаксическая ошибка проверьте наличие скобок и кавычек");

        return _Error.isEmpty();
    }

    private boolean StartScript(){
        Thread th = new Thread(() -> {
            int _ComanNum = 0;
            while(!_IsStop && _ComanNum < _TransScript.size() && _Error.isEmpty()) {
                String _NextComand = _TransScript.get(_ComanNum);
                SetShape(_ComanNum++);

                if(_NextComand.trim().isEmpty()) continue;

                if (_CycleStack.size() != 0){
                    int _FirstStr = _CycleStack.peek()._FirstString;
                    int _EndStr = _CycleStack.peek()._LastString;
                    int _CycCount = _CycleStack.peek()._Counter ;
                    String _CycItem = _CycleStack.peek()._Item;

                    if (_EndStr == _ComanNum -  1){
                        _CycleStack.peek()._Counter --;
                        if(_CycCount == 0) {
                            _CycleStack.pop();
                            _ComanNum = _EndStr + 1;
                            continue;
                        }
                        _ComanNum = _FirstStr + 1;
                        continue;
                    }
                    if(!_CycItem.trim().isEmpty())
                        _NextComand = _NextComand.replaceAll(_CycItem,_CycItem + "[" + _CycCount + "]");

                }


                if(_NextComand.startsWith("Загрузить")){
                    if(_NextComand.length() < 14){_Error.add("Не верный синтаксис команды 'Загрузить'");  break; }
                    int _count = 0;
                    while (_LoadState != Worker.State.SUCCEEDED && _count < 11) {
                        _LogText.setValue(_LogText.getValue() == null ? "": _LogText.getValue() + " Загрузка адреса попытка " + _count++ + "\n");
                        callback.Refresh(_NextComand.substring(11, _NextComand.length() - 3).trim());
                        _LoadState = Worker.State.RUNNING ;
                        while (_LoadState == Worker.State.RUNNING) {
                            Wait(500);
                        }
                    }
                    if(_count > 10) {
                        _Error.add("Не удадось загрузить страницу c " + (_count - 1) + " попыток \n" );
                        break;
                    }

                    _LoadState = Worker.State.RUNNING ;
                    Wait(1000);
                    continue;
                }

                if(_NextComand.startsWith("ДляВсех")){
                    String _valName = _NextComand.substring(7,_NextComand.length()-1).trim();
                    int _FisrstCycle = _ComanNum -1 ;
                    int _EndCycle = GetCycleEnd(_ComanNum -1);
                    _CycleStack.push(new Cycle(-1  ,_FisrstCycle,_EndCycle,_valName));

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            int _CycleCount = (int) _wE.executeScript(_valName +".length") - 1;
                            _CycleStack.peek()._Counter = _CycleCount;
                        }
                    });
                    int _c = 100;
                    while((_CycleStack.peek()._Counter == -1) && (_c > 0)){
                        _c--;
                        Wait(100);
                        System.out.println(_c + "_CycleStack.peek()._Counter = " + _CycleStack.peek()._Counter);
                    }
                    if(_CycleStack.peek()._Counter == -1)
                        _ComanNum = _CycleStack.pop()._LastString;
                    continue;
                }

                if(_NextComand.startsWith("Повторить")){
                    int _CycleNum = Integer.valueOf(_NextComand.substring(10,_NextComand.length() - (_NextComand.endsWith("{") ? 1:0) ).trim());
                    int _FisrstCycle = _ComanNum -1 ;
                    int _EndCycle = GetCycleEnd(_ComanNum -1);
                    _CycleStack.push(new Cycle(_CycleNum  ,_FisrstCycle,_EndCycle,""));
                    continue;
                }

                if(_NextComand.startsWith("Покинуть")){
                    _ComanNum = _CycleStack.peek()._LastString;
                    _CycleStack.pop();
                    continue;
                }

                if(_NextComand.startsWith("Ждать")){
                    Long _mls;
                    if(_NextComand.endsWith(";"))
                        _mls = Long.valueOf(_NextComand.substring(6,_NextComand.length()-2).trim());
                    else _mls = Long.valueOf(_NextComand.substring(6,_NextComand.length()-1).trim());
                    Wait(_mls);
                    continue;
                }

                if(_NextComand.startsWith("if")){
                    _IsIF = -1;
                    _isError = false;
                    String _Condition = _NextComand.substring(2,_NextComand.length()- 1);
                    int _EndCycle = GetCycleEnd(_ComanNum -1);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                boolean _c = (boolean) _wE.executeScript(_Condition);
                                _IsIF = _c ? 1 : 0;
                            } catch (Exception e){
                                System.out.println( "Exception");
                                System.out.println( "Ошибка выполнения команды " + _Condition + "\n" + e.getMessage() + "\n");
                                _Error.add("Ошибка выполнения команды " + _Condition + "\n" + e.getMessage() + "\n");
                                _isError = true;
                            }
                        }
                    });
                    while(_IsIF == -1){
                        Wait(100);
                        if(_isError)
                            break;
                    }
                    System.out.println(_ComanNum + " : " + _Condition + " = " + _IsIF);
                    if(_IsIF == -1) break;
                    if(_IsIF == 0) _ComanNum = _EndCycle;
                        continue;

                }

                if(_NextComand.startsWith(("Остановить"))) {
                    _IsStop = true;
                    break;
                }

                Wait(1000);
                System.out.println(_ComanNum + " : " +_NextComand);
                if(_NextComand.equals("};") || _NextComand.equals("{") || _NextComand.equals(";") || _NextComand.equals("{};")) continue;
                    else {
                    ExecuteCommand(_NextComand);
                }

            }
        if(_IsStop) _Error.add("Остановлено пользователем \n");
            else{
                if(_Error.isEmpty())
                    _Error.add("Выполнение алгоритма завершено \n");
                else
                    _Error.add("Ошибка выполнения алгоритма! \n");
        }
        set_IsStop(true);

        callback.OnStoped();

        });
        th.start();

        return _Error.isEmpty();
    }

    public String GetTranspilationString(int _StringNum){
        if(_StringNum > _TransScript.size()) return "Не верный номер стороки";
        return _TransScript.get(_StringNum -1);
    }

    public void registerCallBack(Callback callback){
        this.callback = callback;
    }

    private void Wait(long mls){
        try {
            Thread.sleep(mls);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void SetShape(int _Pos){
        SCRIPTTABLE.getSelectionModel().select(_Pos);
        if(_Pos > _ScrollString){
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    SCRIPTTABLE.scrollTo(_Pos);//_ScrolCounter++);
                    //_ScrollString  += _ScrollString;

                }
            });
        }
    }

    public static void set_LoadState(Worker.State _LoadState) {
        Script._LoadState = _LoadState;
    }

    private void ExecuteCommand(String _Command){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    _wE.executeScript(_Command);
                }catch (Exception e){
                    _Error.add("Ошибка выполнения команды " + _Command + "\n" + e.getMessage());
                }
            }
        });
    }

    private int GetCycleEnd(int _TarnsString){
        int _BracketsCounter = 0;
        int  i;
        for(i = _TarnsString; i < _TransScript.size(); i++){
            if(_TransScript.get(i).contains("{")) _BracketsCounter ++;
            if(_TransScript.get(i).contains("}")) _BracketsCounter --;
            if(_BracketsCounter == 0) break;
        }

        return i;

    }

    public static boolean CheckNumber(String _String){
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(_String);
        return m.matches();
    }

    public void setSCRIPTTABLE(TableView SCRIPTTABLE) {
        this.SCRIPTTABLE = SCRIPTTABLE;
    }
}



















