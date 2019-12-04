import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BeatBox {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList; // список флажков
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
            "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga" };
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
    /*
     * ^ Эти числа - фактические барабанные клавиши. Канал барабана - это что-то вроде фортепиано, только каждая
     * клавиша на нем - отдельный барабан. Номер 35 - клавиша для Bass drum, а 42 - Closed Hi-Hat и т.д
     */

    public static void main (String[] args) {
        new BeatBox().buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // ^ Пустая граница позволяет создать поля между краями панели и местами размещений компонентов

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        /* Создаем флажки,присваиваем им значения false(чтобы они не были установлены),
         * а затем добавляем их в массив ArrayList и на панель
         */
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4); // Стандартный MIDI-код для получения синтезатора,
            track = sequence.createTrack();                     // секвенсора и дорожки.
            sequencer.setTempoInBPM(120);

        } catch (Exception e) { e.printStackTrace(); }
    }

    public void buildTrackAndStart() {
        /* Создаем массив из 16 элементов,чтобы хранить значения для каждого инструмента
          на все 16 тактов */
        int[] trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack(); // Избавляемся от старой дорожки и создаем новую

        for (int i = 0; i < 16; i++) { // <- Делаем это для каждого из 16 рядов
            trackList = new int[16];

            int key = instruments[i]; // <- Задаем клавишу,которая представляет инструмент из заданного в начале массива

            /* Установлен ли влажок на этом такте? Если да,то помещаем значения клавиши в текущую ячейку массива
             * (ячейку,которая представляет такт). Если нет,то инструмент не должен играть в этом такте,поэтому
             * присвоим ему 0. */
            for(int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            } //внутренний цикл

            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
        } //внешний цикл

        track.add(makeEvent(192, 9, 1, 0, 15));
        try {

            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch(Exception e) { e.printStackTrace(); }
    }

    // Внутренние классы - слушатели для кнопок.
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * .97));
        }
    }


    /* Метод создает события для одного инструмента за каждый проход цикла для всех 16 тактов.
     *  Можно получить int[] для Bass drum, и каждый элемент массива будет содержать либо клавишу
     *  этого инструмента,либо ноль. Если это ноль, то инструмент не должен играть на текущем такте.
     *  Иначе нужно создать событие и добавить его в дорожку */
    public void makeTracks(int[] list) {
        for (int i = 0; i < 16; i++) {
            int key = list[i];

            if (key != 0) {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(144, 9, key, 100, i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) { e.printStackTrace(); }
        return event;
    }
}