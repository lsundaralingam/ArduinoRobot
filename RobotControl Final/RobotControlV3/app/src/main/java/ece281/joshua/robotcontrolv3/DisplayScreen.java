package ece281.joshua.robotcontrolv3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class DisplayScreen extends View {

    int distance;
    Paint paint;

    public DisplayScreen(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        // make a new paint object
        paint = new Paint();
    }
    public void updateDisplay(int distance){
        // Set the distance we get as the parameter to the distance variable declared in this class
        this.distance = distance;
        this.invalidate(); //Forces the view to redraw, calling onDraw()
    }
    public void onDraw(Canvas canvas){
        int blockY;
        // Note: the blockY value is the length of foreground rectangle 
        // If distance is greater than 100 then we reset the blockY to be 0 so the graph doesn't show on screen.
        if(distance >= 2*50){
            blockY = 0;
        }
        // Otherwise, the blockY is calculated using the formula below
        else{
            blockY = ((2*50 - distance) * 12);
        }
        // We fill the rectangle with a color
        paint.setStyle(Paint.Style.FILL);
        // Draw a rectangle, which is basically our graph based on the blockY value
        Rect foreground = new Rect(0, 0, 1200, blockY);
        super.onDraw(canvas);

        // if the distance is greater than 60, make the color of the foreground rectangle to be Yellow
        if(distance >= 2*30){
            paint.setColor(Color.YELLOW);
            canvas.drawRect(foreground, paint);
        }
        // if the distance is between 60 and 30, make the color of the foreground rectangle to be Orange
        else if(distance < 2*30 && distance >= 2*15){
            //Set foreground rect color to orange
            paint.setColor(Color.rgb(255,165,0));
            canvas.drawRect(foreground, paint);
        }
        // if the distance is lower than 30 or any other value, make the color of the foreground rectangle to be RED
        else{
            paint.setColor(Color.RED);
            canvas.drawRect(foreground, paint);
        }

        paint.setColor(Color.BLACK); // Set the font color to be Black
        paint.setTextSize(35); // Set the font size to 35

        // Write the label onto the screen
        canvas.drawText("                               Distance from Obstacle (in cm)                                                  ", 10, 30, paint);

        // Change the Size of the font to be 45
        paint.setTextSize(45);

        // Setup the Scale for the graph
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 90 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 100, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 80 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 217, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 70 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 334, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 60 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 451, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 50 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 568, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 40 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 685, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 30 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 802, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 20 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 919, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - - 10 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 1036, paint);
        canvas.drawText("- - - - - - - - - - - - - - - - - - - - -  0 - - - - - - - - - - - - - - - - - - - - - - - - - ", 10, 1153, paint);
    }
}