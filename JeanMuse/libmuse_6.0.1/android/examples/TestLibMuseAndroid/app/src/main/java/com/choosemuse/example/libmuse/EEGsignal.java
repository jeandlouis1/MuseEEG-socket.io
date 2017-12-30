package com.choosemuse.example.libmuse;

/**
 * Created by Jean on 12/28/2017.
 */

public class EEGsignal {
    private String name;
    private String elem1;
    private String elem2;
    private String elem3;
    private String elem4;

    public EEGsignal(String name){
        this.name =name;
    }

    public String getElem1() {
        return elem1;
    }

    public void setElem1(String elem1) {
        this.elem1 = elem1;
    }

    public String getElem2() {
        return elem2;
    }

    public void setElem2(String elem2) {
        this.elem2 = elem2;
    }

    public String getElem3() {
        return elem3;
    }

    public void setElem3(String elem3) {
        this.elem3 = elem3;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getElem4() {
        return elem4;
    }

    public void setElem4(String elem4) {
        this.elem4 = elem4;
    }

    @Override
    public String toString() {
        return "EEGsignal{" +
                "name='" + name + '\'' +
                ", elem1='" + elem1 + '\'' +
                ", elem2='" + elem2 + '\'' +
                ", elem3='" + elem3 + '\'' +
                ", elem4='" + elem4 + '\'' +
                '}';
    }
}
