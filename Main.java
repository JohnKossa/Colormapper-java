/**
 *Vavoom
 */
import java.io.*;
import java.awt.*;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.Math;
public class Main
{
    public static double spatialDist(int x1, int y1, int x2, int y2)
    {
        double dist = (x1-x2) * (x1-x2);
        dist += (y1-y2) * (y1-y2);
        return Math.sqrt(dist);
    }
    public static double dataDist(int color1, int color2)
    {
        int r1 = (color1 & 0x00ff0000) >> 16;
        int g1 = (color1 & 0x0000ff00) >> 8;
        int b1 =  color1 & 0x000000ff;
        int r2 = (color2 & 0x00ff0000) >> 16;
        int g2 = (color2 & 0x0000ff00) >> 8;
        int b2 =  color2 & 0x000000ff;
        double dist = (r1 - r2) * (r1 - r2);
        dist += (g1 - g2) * (g1 - g2);
        dist += (b1 - b2) * (b1 - b2);
        return Math.sqrt(dist);
    }
    public static int[][] propogateChange(int[][]map, int w, int h, int p, int x, int y)
    {
        int a = 1;
        double d;
        int color = map[x][y];
        for(int i = 0; i<w; i++)
        {
            for(int ii = 0; ii<h; ii++)
            {
                d = spatialDist(x, y, i, ii);
                int rDif = (((color & 0x00ff0000) >> 16) - ((map[i][ii] & 0x00ff0000) >> 16))/2;
                int gDif = (((color & 0x0000ff00) >> 8) - ((map[i][ii] & 0x0000ff00) >> 8))/2;
                int bDif = ((color & 0x000000ff) - (map[i][ii] & 0x000000ff))/2;
                rDif *= Math.exp((-(d*d)/5));
                gDif *= Math.exp((-(d*d)/5));
                bDif *= Math.exp((-(d*d)/5));
                rDif *= (1/(p+1));
                gDif *= (1/(p+1));
                bDif *= (1/(p+1));
                map[i][ii] += ((rDif << 16) & 0x00ff0000) | ((gDif << 8) & 0x0000ff00) | (bDif & 0x000000ff);  //distance factor * change amount
            }
        }
        return map;
    }
    public static int[] bestMatch(int[][] map, int w, int h, int color)
    {
        int[] lowest = new int[2];
        double lowdist = dataDist(map[0][0], color);
        double temp;
        for(int i = 0; i<w; i++)
        {
            for(int ii = 0; ii<h; ii++)
            {
                temp = dataDist(map[i][ii], color);
                if(temp < lowdist)
                {
                    lowdist = temp;
                    lowest[0]= i;
                    lowest[1] = ii;
                }
            }
        }
        return lowest;
    }
    public static void main(String [] args)throws Exception
    {
        File source= new File("image.jpg");
        BufferedImage sourceImage = ImageIO.read(source);
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int clr;
        int depth = 4;
        int[][] inputMap = new int[width][height];
        int[][] trainingMap = new int[width][height];
        int[][] outputMap = new int[width][height];
        Random rand = new Random();
        //Store initial image
        for(int i = 0; i<width; i++)
        {
            for(int ii = 0; ii<height; ii++)
            {
                clr = sourceImage.getRGB(i,ii);
                inputMap[i][ii] = clr;                          //initial map
                int r = 128*(width-i)/width + 128*ii/height;
                int g = 128*(width-i)/width + 128*(height-ii)/height;
                int b = 128*i/width + 128*(height-ii)/height;
                trainingMap[i][ii] = (r << 16) | (g << 8) | b; 
                outputMap[i][ii] = clr;                         //final map = initial map
            }
        }
        System.out.println("Reading complete");
        //Teach map
        File tMap = new File("imagetraining.jpg");
        BufferedImage tImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int z = 0; z<depth; z++)
        {
            for(int i = 0; i<width; i++)
            {
                for(int ii = 0; ii<height; ii++)
                {
                    int[] bestPixel = bestMatch(trainingMap, width, height, inputMap[i][ii]);                               //get the closest matching pixel
                    trainingMap = propogateChange(trainingMap, width, height, z, bestPixel[0], bestPixel[1]); //propogate change
                    tImage.setRGB(i, ii, trainingMap[i][ii]);
                }
                System.out.println("Training Row #"+i+" Pass #"+(z+1)+" completed");
            }
        }
        ImageIO.write(tImage, "jpg", tMap);//write map file
        //Rebuild
        File fMap = new File("imagerebuild.jpg");
        BufferedImage fImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int i = 0; i<width; i++)
        {
            for(int ii = 0; ii<height; ii++)
            {
                int[] bestPixel = bestMatch(trainingMap, width, height, inputMap[i][ii]);   //find best pixel
                fImage.setRGB(i, ii, trainingMap[bestPixel[0]][bestPixel[1]]);//place color
            }
            System.out.println("Building Row #"+i+" completed");
        }
        ImageIO.write(fImage, "jpg", fMap); //write map file//write final file
    }
}
