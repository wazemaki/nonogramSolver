package hu.unideb.inf.nonogramsolver.Controller;

import hu.unideb.inf.nonogramsolver.Model.Drawing.RedrawTask;
import hu.unideb.inf.nonogramsolver.Model.Drawing.DrawingData;
import hu.unideb.inf.nonogramsolver.Model.Solver.Solver;
import hu.unideb.inf.nonogramsolver.Model.PuzzleRawData;
import hu.unideb.inf.nonogramsolver.Model.Solver.SolverEvent;
import hu.unideb.inf.nonogramsolver.Model.SolverException;
/**
 * A fejtő vezérlését végzi.
 * @author wazemaki
 */
public class SolverController {
    
    private SolverEvent<String> startEvent;
    private SolverEvent<String> endEvent;
    private SolverEvent<String> errorEvent;
    private SolverEvent<String> completeEvent;
    private SolverEvent<String> stoppedEvent;
    private RedrawTask redrawEvent;
    
    private final DrawingData drawData;
    private Solver puzzle;
    private Thread thread;
    private PuzzleRawData rawData;
    
    /**
     * Konstruktor.
     */
    public SolverController(){
        this.drawData = new DrawingData();
        this.rawData = new PuzzleRawData();
    }
    
    /**
     * Eseményt állít be a fejtőnek.
     * @param eventType Az esemény típusa.
     * @param event Az esemény objektuma.
     * @return
     */
    public boolean setEvent(byte eventType, SolverEvent event){
        switch(eventType){
            case SolverEvent.EVENT_START:
                this.startEvent = event;
                return true;
            case SolverEvent.EVENT_END:
                this.endEvent = event;
                return true;
            case SolverEvent.EVENT_ERROR:
                this.errorEvent = event;
                return true;
            case SolverEvent.EVENT_COMPLETE:
                this.completeEvent = event;
                return true;
            case SolverEvent.EVENT_STOP:
                this.stoppedEvent = event;
                return true;
            case SolverEvent.EVENT_REDRAW:
                this.redrawEvent = new RedrawTask() {
                    @Override
                    protected void redraw(DrawingData data) {
                        event.run(data);
                    }
                };
                return true;
        }
        return false;
    }
    
    /**
     * A rejtvényt állítja be.
     * @param rawData A rejtvény objektuma.
     */
    public void setRawData(PuzzleRawData rawData){
        this.rawData = rawData;
    }
    
    /**
     * Leellenőrzi, hogy a betöltött rejtvény nem-e üres, vagy hibás.
     * @return Igaz{@code true}, ha a rejtvény érvényes.
     * Hamis{@code false}, ha a rejtvény nem érvényes.
     */
    public boolean isValidPuzzle(){
        return !(this.rawData == null || this.rawData.isEmpty());
    }
    
    /**
     * A nyers rejtvényt adja vissza.
     * @return A nyers rejtvény.
     */
    public PuzzleRawData getRawData(){
        return this.rawData;
    }
    
    /**
     * A rejtvény szélességét (oszlopok számát) adja vissza.
     * @return Szélesség.
     */
    public int getSizeX(){
        return this.rawData.getSizeX();
    }
    
    /**
     * A rejtvény magasságát (sorok számát) adja vissza.
     * @return Magasság.
     */
    public int getSizeY(){
        return this.rawData.getSizeY();
    }
    
    /**
     * A betöltött rejtvény megfejtését indítja el.
     * @param enBackup Visszalépés(tippelés) engedélyezése
     * @param enPrior Prioritás szerinti sorválasztás engedélyezése.
     * @throws SolverException
     */
    public void solve(boolean enBackup, boolean enPrior) throws SolverException{
        if(!this.checkIsFree()){
            throw new SolverException("A fejtő foglalt.\n  Várjuk meg, amíg befejezi, vagy állítsuk meg!",3);
        }
        if(!this.isValidPuzzle()){
            throw new SolverException("Érvénytelen rejtvény, valószínűleg sorok, vagy oszlopok hiányoznak.",2);
        }
        this.puzzle = new Solver(this.rawData.getCols(),this.rawData.getRows(),this,enBackup,enPrior);
        this.thread = new Thread (this.puzzle, "Solving");
        this.thread.start();
    }
    
    /**
     * A fejtő objektumot adja vissza.
     * @return A fejtő.
     */
    public Solver getSolver(){
        return this.puzzle;
    }
    
    /**
     * Leellenőrzi, hogy a fejtő szabad-e, azaz fut-e épp fejtés.
     * @return Igaz{@code true}, ha a fejtő szabad.
     * Hamis{@code false}, ha a fejtő nem szabad.
     */
    public boolean checkIsFree(){
        return (this.thread == null || !this.thread.isAlive());
    }
    
    /**
     * Megállítja a fejtést.
     */
    public void stopSolving(){
        if(this.thread != null && this.puzzle != null){
            this.puzzle.stop();
        }
    }
    
    /**
     * "Complete" eseményt hív meg.
     * @param p A paraméter, amely átadódik az eseménynek.
     */
    public void callOnComplete(String p){
        if(this.completeEvent != null){
            this.completeEvent.run(p);
        }
    }
    
    /**
     * "Error" eseményt hív meg.
     * @param p A paraméter, amely átadódik az eseménynek.
     */
    public void callOnError(String p){
        if(this.errorEvent != null){
            this.errorEvent.run(p);
        }
    }
    
    /**
     * "Start" eseményt hív meg.
     * @param p A paraméter, amely átadódik az eseménynek.
     */
    public void callOnStart(String p){
        if(this.startEvent != null){
            this.startEvent.run(p);
        }
    }
    
    /**
     * "Stop" eseményt hív meg.
     * @param p A paraméter, amely átadódik az eseménynek.
     */
    public void callOnStopped(String p){
        if(this.stoppedEvent != null){
            this.stoppedEvent.run(p);
        }
    }
    
    /**
     * "End" eseményt hív meg.
     * @param p A paraméter, amely átadódik az eseménynek.
     */
    public void callOnEnd(String p){
        if(this.endEvent != null){
            this.endEvent.run(p);
        }
        this.thread = null;
    }
    
    /**
     * "Redraw" eseményt hív meg.
     */
    public void callOnRedraw(){
        if(this.redrawEvent != null && this.puzzle != null){
            this.drawData.setData(this.puzzle.getGrid(), this.puzzle.getIsRow(), this.puzzle.getActiveLine());
            this.redrawEvent.run(this.drawData);
        }
    }
}
