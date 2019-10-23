package oglib;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.io.IOException;

import oglib.components.CompileException;
import oglib.components.CreateException;
import oglib.components.Program;
import oglib.game.GameState;
import oglib.gui.Simple2DBuffer;
import oglib.gui.WindowGL;

public class App {
    public static void main(String[] args) {
        var gameState = GameState.getGameState();
        var width = 300;
        var height = 300;
        var w = new WindowGL(width, height, "Drawing Program", gameState);

        try {
            var program = new Program("screen.vert", "screen.frag");
            var screen = new Simple2DBuffer(width, height);
            //Hacen linea horizontal
            //var x = 255; //color 
            //for (int i = 0; i < width; i++) {
            //    screen.set(i, 200, x, x, x); //al monitor le digo que coloque el color en posicion i xxx rgb
            //}

            drawLine(screen, 10, 100, 11,200);
            drawCircle(screen, 100, 100, 50);

            while (!w.windowShouldClose()) {
                glClearColor(0f, 0f, 0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                program.use();
                screen.draw(); //dibujar como textura
                w.swapBuffers();
                w.pollEvents();
            }
            w.destroy();
        } catch (IOException | CreateException | CompileException e) {
            e.printStackTrace();
        }

    }


//-------------------------------------------------------------------
//Función para el dibujo de la linea 
//--------------------------------------------------------------------

//Función DDA (Analizador de diferencia digital)
/*
Para formar la pendiente dados 2 puntos generalmente se unen para formar un segmento de recta 
en este caso necesitamos calcular punto por punto intermedio para generar una linea 

Tomamos en cuenta la ecuación de la pendiente de linea
y=mx+b
donde m es la pendiente y b es y- m(x1)
m=dy/dx  es decir m=y2-y1/x2-x1

cuando m<1 significa que x aumenta mas que y
cuando m>1 significa que y aumenta mas que x

Entonces los pasos generales para obtener la linea son:
1. Ingresar como parametros de entrada posicion del primer punto y la posicion del punto final
(se dibuja de izquierda a derecha)
2. Calcular dx y dy 
3. Dependiendo del valor absoluto de dx y dy se elige el número de pasos 
4. Se calcula el incremento en x y de y para cada paso
5. Se ponen los píxeles para cada paso

Ejemplo:
Consideramos A(0, 0) -->primer punto y B(8, 4) -->ultimo punto
Primeramente calculamos 
dx= 8-0= 8 
dy= 4-0= 4

Ahora el valor absoluto de dx es mayor a el valor absoluto de dy
Si entonces utilizamos el 8 como pasos 
Y calculamos el incremento 
xinc= dx/pasos= 8/8= 1
yinc= dy/pasos= 4/8= 0.5

despues continuamos con el incremento tomando las coordenadas siguientes
x1=x0+ xinc --> 0+1= 1
y1=y0+ yinc --> 0+0.5 =0.5
(1, 0.5)
siguiente
x2=x1+ xinc --> 1+1= 2
y2=y1+ yinc --> 0.5+0.5 =1
(2, 1)

Asi con todos los valores hasta llegar a la coordenada final o el ultimo punto que es B(8, 4)

*/

public static float valorAbs (float n) { //Regresar el valor absoluto
    if (n>0){
        return n;
    }
    else {
        return (n*(-1));
    }
} 

public static void drawLine(Simple2DBuffer screen, int X0, int Y0, int X1, int Y1) { 
    //Calcular dx y dy
    float dx= X1-X0; 
    float dy= Y1-Y0; 
    //Calcular paso por paso para general pixeles
    float pasos;
        if(valorAbs(dx)>valorAbs(dy)){
            pasos=valorAbs(dx);
        }
        else{
            pasos=valorAbs(dy); 
        }
  
    //Incremento de x y y dependiento de los pasos
    float Xinc= dx/(float) pasos; 
    float Yinc= dy/(float) pasos; 
  
    //Poner un pixel por cada paso
    float x= X0; 
    float y= Y0; 
    var z= 250;
    for (int i=0; i<=pasos; i++){
        screen.set(Math.round(x),  Math.round(y), z, z, z); //al monitor le digo que coloque el color en posicion i xxx rgb
        x += Xinc; //Incremento en x
        y += Yinc; //Incremento en y
        //Paso por paso hasta llegar al punto final
    } 

}

//-------------------------------------------------------------------
//Función para el dibujo del circulo
//--------------------------------------------------------------------

//Función Bresenham
/**
Para inicializar el algortimo de Bresenham comenzamos con 8 puntos que se pintan por simetria

Usamos la funcion del circulo =x^2+y^2-r^2

Para cada pixel tomamos 3 esquinas del cuadrado:
esquina1= x, y
esquina2= x+1, y 
esquina3= x+1, y-1

Y a los pixeles se les toma la distancia de cada punto hacia la circunferencia

d1= distancia del pixel de fuera hacia dentro hasta tocar con la circunferencia
Es decir la distancia de esquina3 al circulo d1= (x +1)^2 + y^2 - r^2
d2= distancia del pixel de adentro hacia afuera hasta tocar la circuferencia
Es decir la distancia de esquina2 al circulo d2= (x +1)^2 + (y-1)^2 - r^2

Para este algoritmo necesitamos obtener un parametro de desicion p=d1+d2
Donde obtenemos p1= 2(x+1)^2 + y^2 +(y-1)^2 - 2r^2

Sustituimos valores y si p<0 se debe usar la esquina3 por que esta mas cerca
y la mas factible para usar y con ello la formula d= d+4x+6

De otra forma si p>=0 se debe usar la esquina2 por que esta mas cerca 
y con ello la formula d= d+4x-y+10

Entonces los pasos generales para obtener el circulo son:
1. Como primer paso ingresamos el radio y el centro del circulo
2. Despues calculamos el valor inicial con 3-r2
3. Para el siguiente paso calculamos el test p<0 o >=0 con respecto a ello utilizamos las formulas correspondientes 
4. Despues determinamos la simetria de los siguientes puntos graficados
5. Finalmente movemos los pixeles en un camino circular asi hasta que x sea mayor a y (se salga del while)

Ejemplo dado un r=10
Obtenemos p=3-2r= 3-20= -17  ---> (1,10) 
Eso significa que p<0 por lo que se ocupa esquina3
Donde el incremento inicial es con la formula de esquina3 
d= d+4x+6 -> d=-17+4(0)+6 =-11  ---> (2,10)

Da -11 p<0, significa nuevamente la formula de equina3
d= d+4x+6 -> d=-11+4(1)+6 =-1   ---> (3,10)

Asi susecivamente hasta llegar a todas las coordenadas

*/

public static void puntosSimetricos(Simple2DBuffer screen, int centroX, int centroY, int x, int y){  
    //8 Puntos usando simetria de 45 grados
    //centro, centroX y centroY //pixel activo x y y
    var z= 250;
    screen.set(x + centroX, y + centroY, z, z, z);  
    screen.set(x + centroX,-y + centroY, z, z, z);  
    screen.set(-x+ centroX,-y + centroY, z, z, z);  
    screen.set(-x+ centroX, y + centroY, z, z, z);  
    screen.set(y + centroX, x + centroY, z, z, z);  
    screen.set(y + centroX,-x + centroY, z, z, z);  
    screen.set(-y+ centroX,-x + centroY, z, z, z);  
    screen.set(-y+ centroX, x + centroY, z, z, z);  
}  
  
public static void drawCircle(Simple2DBuffer screen, int centroX, int centroY, int r)  {
    //xc coordenadas dentro del circulo
    //radio del circulo
    ///En nuestro primer cuadrante del Circulo va desde x=0 hasta x=y con una pendiente que va desde -1 a 0
    int x=0;
    int y=r;
    int d=3-(2*r);
    
    /*
    Como obtener 3-2r
    xi=0 Desicion inicial
    di=(xi-1+1)2+ yi-12 -r2+(xi-1+1)2+(yi-1 -1)2-r2
    di=(0+1)2+r2 -r2+(0+1)2+(r-1)2-r2
    =1+1+r2-2r+1-r2
    = 3 - 2r
    */
    
    puntosSimetricos(screen, centroX,centroY, x, y);  

    while(x<=y){  //Circulo escaneado y convertido
        //Ubicacion de los siguientes pixeles a escanear
        if(d<=0){  //Se utiliza esquina3= x+1, y-1, para que sea el siguiente pixel
            d=d+(4*x)+6;  
        }  
        else{  //Se utiliza esquina2= x+1, y, para que sea el siguiente pixel
            d=d+(4*x)-(4*y)+10;  
            y=y-1;  
        }  

        /*
        x,y
        <0, dentro del circulo 
        =0, sobre el circulo 
        >0, fuera del circulo
        */

        x=x+1;  
        puntosSimetricos(screen, centroX, centroY, x, y);  
        }  
    }

/* Como sacar las formulas de cada esquina d=d+(4*x)-(4*y)+10 y d=d+(4*x)+6; 
Sacamos diferencia entre ecuaciones p1-p para sacar los valores (p seria p1 con signos opuestos)
dif=2(x+1)^2 + y^2 +(y-1)^2 - 2r^2 -menos- 2(x+1)^2 - y^2 - (y-1)^2 + 2r^2
y lo que resulta de la direrencia es
dif= p +4x+2(y^2-y^2) -2(y-y)+6
Cuando se utiliza la esquina3 la "y" permanece igual como "y", 
pero cuando se utiliza esquina2 "y" se convierte en "y-1", 
sustituimos y obtenemos sus formulas correspondientes 
*/

/*
//MIDPOINT CIRCLE 

private static void drawCircle(Simple2DBuffer screen, final int centerX, final int centerY, final int radius) {
		int x = 0;
        int y = radius;
        int d = (5 - radius * 4) / 4;
        //int d = 3 - 2 * r; 
        var z = 250;
        
		do {
            //screen.set((int) x, (int) y, z, z, z)
			screen.set(centerX + x, centerY + y, z, z, z);
			screen.set(centerX + x, centerY - y, z, z, z);
			screen.set(centerX - x, centerY + y, z, z, z);
            screen.set(centerX - x, centerY - y, z, z, z);
			screen.set(centerX + y, centerY + x, z, z, z);
			screen.set(centerX + y, centerY - x, z, z, z);
			screen.set(centerX - y, centerY + x, z, z, z);
			screen.set(centerX - y, centerY - x, z, z, z);
			if (d < 0) { //actualizacion de d, x, y
				d += 2 * x + 1;
			} else {
				d += 2 * (x - y) + 1;
				y--;
			}
			x++; //cada pixel se dibuja 
		} while (x <= y);
 

}*/


} 








