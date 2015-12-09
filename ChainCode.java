/*
Jessica Temporal			Nº USP: 7547611
Matheus Calil Faleiros		Nº USP: 8505053
Natália Baptista Cruz		Nº USP: 4624319
Vitor Valsichi Cuziol		Nº USP: 8505081
*/

import java.awt.*;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.Arrays;
import ij.*;


/**
	Classe com metodos envolvidos no calculo do codigo da cadeia.
*/
public class ChainCode {


	/**
	Encontra os pixels do contorno da imagem (objeto branco e fundo preto)
	e retorna vetor com coordenadas x e y de cada ponto do contorno.
	*/
	public static int[][] outlinePixels(ImageAccess input){
		
		int k = 0;									// numero de pixels no contorno
		double[][] neighbors = new double[3][3];	// 3x3
		
		// conta o numero de pixels no contorno
		for (int i = 0; i < input.getWidth(); i++)		
			for (int j = 0; j < input.getHeight(); j++)	
				if (input.getPixel(i,j) >= 0.5){
					
					input.getNeighborhood(i,j,neighbors);
					
					if(neighbors[0][1] <= 0.5 ||
					neighbors[1][0] <= 0.5 ||
					neighbors[1][2] <= 0.5 ||
					neighbors[2][1] <= 0.5
					){
						k++; // conta o pixel se ele for branco e tiver pelo menos um preto nas 4 direcoes
					}
				}
		
		// guarda as coordenadas dos pixels no contorno
		int[][] s = new int[2][k];
		k = 0;
		for (int i = 0; i < input.getWidth(); i++)
			for (int j = 0; j < input.getHeight(); j++)
				// verifica se o pixel é do contorno
				if (input.getPixel(i,j) >= 0.5){
				
					input.getNeighborhood(i,j,neighbors);
					
					if (
					neighbors[0][1] <= 0.5 ||
					neighbors[1][0] <= 0.5 || 
					neighbors[1][2] <= 0.5 ||
					neighbors[2][1] <= 0.5 
					){
						s[0][k] = i; // armazena as coordenadas do pixel...
						s[1][k] = j; // ...se ele for branco e tiver pelo menos um preto na fronteira
						k++;
					}
				}
	
		return s;
				
	}
//----------------------------------------------------------------------------------------------------
	/**
	Gera uma imagem contorno dado os pixels do contorno e o tamanho da imagem.
	*/
	public static ImageAccess outlineImage(int[][] s, int nx, int ny){
		ImageAccess outline = new ImageAccess(nx, ny);
		
		// preenche com preto uma nova imagem com apenas o contorno da imagem original
		for(int i = 0; i < nx; i++)
			for(int j = 0; j < ny; j++)
				outline.putPixel(i, j, 0);
		
		// preenche com branco o contorno da imagem
		for(int i = 0; i < s[0].length; i++){
			outline.putPixel(s[0][i], s[1][i], 1);
		}
	   
		return outline;
	}
//----------------------------------------------------------------------------------------------------
/**
	Retorna a imagem reamostrada.
	
	@param rate 			valor da janela de reamostragem 
	@param input 			imagem original (contorno)
	*/
	public static ImageAccess resample(ImageAccess input, int rate){
		
		//Pega a imagem original
		//pega o tamanho da grade de reamostragem
		//Para o tamanho da grade de reamostragem, verificar se algum pixel esta pintado
		//Se estiver, pintar 1 pixel na imagem reamostrada
		//Se não estiver, proxima vizinhança
		
		int width = input.getWidth();		//largura da imagem
		int height = input.getHeight();		//altura da imagem
		ImageAccess output = new ImageAccess(width/rate, height/rate);
		int index = rate;
		double[][] neighbors = new double[index][index];
		
		if(rate == 1) // se rate = 1, não tem reamostragem
			return input;

		// anda na imagem de acordo com a janela de reamostragem
		for(int i=0, m=0; i< width; i=i+index, m++){
			for(int j=0, n=0; j<height; j=j+index, n++){
			
				// pega vizinhança e anda nela
				input.getNeighborhood(i,j,neighbors);
				
				for(int x = 0; x<index; x++){
					for(int y = 0; y<index; y++){
						
						// se vizinhança tiver um pixel branco, output recebe um pixel branco
						if (neighbors[x][y] != 0){
							output.putPixel(m,n,1);
							x = index; // sai do for aninhado que anda na vizinhança
							y = index;
						}
					}
				}
			}
		}
		
		
		return output;
	
	}

//----------------------------------------------------------------------------------------------------
	/**
	Verifica se dada coordenada esta dentro dos limites da imagem
	*/
	public static boolean isInside(ImageAccess img, int x, int y) {
		int nx = img.getWidth();
		int ny = img.getHeight();
		
		// coordenada x
		if(x < 0 || x >= nx) {
			return false;
		}
		
		// coordenada y
		if(y < 0 || y >= ny) {
			return false;
		}
		
		return true;
	}
	
//----------------------------------------------------------------------------------------------------
/** Realiza o Codigo da Cadeia para um determinado contorno

	@param outline 			imagem de contorno, depois da reamostragem
	@param s 				vetor contendo indices dos pixeis de contorno
	@return codeFinal 		vetor de caracteristicas do codigo da cadeia
	*/
	public static int[] chainCode(ImageAccess outline, int[][] s) {
		boolean flag;	// usado pra controlar se chegou a um "beco sem saida"
		int c = 0;
		int n = s[0].length;
		
		int startx = s[0][0];
		int x = s[0][0];
		int starty = s[1][0];
		int y = s[1][0];
		
		int mov = 1;
		int dir = 0;
		
		// lista ordenada de coordenadas do contorno
		int[][] coord = new int[2][n];
		
		// para cada direcao (0 a 7), qual o incremento em x e y;
		// lembre-se que a origem eh o canto superior esquerdo da imagem
		int nx[] = {1, 1, 0, -1, -1, -1, 0, 1};
		int ny[] = {0, -1, -1, -1, 0, 1, 1, 1};
		
		// codigo da cadeia
		int[] code = new int[n];
		
		for(int a = 0; a<n; a++) // inicializa vetor code
			code[a] = -1;
		
		// inicializa lista de coordenadas
		for(int i = 0; i < 2; i ++) {
			for(int j = 0; j < n; j++) {
				coord[i][j] = -1;
			}
		}
		
		// seta primeiro pixel do contorno
		coord[0][c] = x;
		coord[1][c] = y;
		
		// enquanto não terminamos o contorno...
		while(c < n && c >= 0) {
			flag = false;
			
			//marca os pixels que já passaram com 0 para não ter o risco de visitar de novo
			outline.putPixel(coord[0][c], coord[1][c], 0); 
			
			// percorrer 8 pixels em volta do pixel atual
			for(int i = 0; i < 8; i++){
				
				// incrementa a direção
				dir = (mov + i) % 8;
				
				// se o pixel que pegarmos estiver dentro da imagem...
				if (isInside(outline, coord[0][c]+nx[dir], coord[1][c]+ny[dir])) {
					// se o pixel for branco...
					if ((outline.getPixel(coord[0][c]+nx[dir], coord[1][c]+ny[dir]) != 0)){
					
						// pega as coordenadas do proximo pixel do contorno
						x = coord[0][c] + nx[dir];
						y = coord[1][c] + ny[dir];
						
						// pega o contrário da direção que seguimos,
						// que será a nova direção inicial (quando for percorrer as 8 direções)
						mov = (dir + 4) % 8;
						
						// adiciona direcao no codigo da cadeia
						code[c] = dir;
						
						// encontrou próximo pixel do contorno...
						flag = true;
						// ... então sai pra ir pro próximo.
						break;
						
					
					}
				}
			}
			
			if(!flag) {
				// volta um pixel, a partir da lista
				c--;
			} else {
				// "proximo pixel" entra na lista ordenada de coordenadas
				c++;
				coord[0][c] = x;
				coord[1][c] = y;
			}
			
			// chegamos ao pixel inicial; entao terminamos de percorrer o contorno.
			if(x == startx && y == starty && c > 0){
				break;
			}
		
		} //-------- while
		
		
		// removendo os valores de ruido do vetor code 
		int contador = 0; // variavel para instanciar o vetor 
		for(int a=0; a<code.length; a++){ 
			if(code[a] > -1) 
				contador++; // conta apenas positivos 
		}
		
		int[] codeFinal = new int[contador];
		for(int i = 0; i < contador; i++){ 
			codeFinal[i] = code[i]; 
		} 
			
		return codeFinal;
		
	} // fim codigo da cadeia
	
//----------------------------------------------------------------------------------------------------
	/** 
	Torna o codigo da cadeia invariante a rotacao.
	*/
	public static int[] invariant(int vet[]){
	    
		int[] v = new int[vet.length];
		int i = 0;
		int diff = 0;

		if (i == 0){ // initial point case
		    diff = (vet[i] - vet[vet.length-1]);
			if (diff < 0){
				v[i] = 8 + diff;
			}else{
				v[i] = diff;
			} // else
		} // if

		for (i=1; i<vet.length; i++){
			diff = (vet[i] - vet[i-1]);
			if (diff < 0){
				v[i] = 8 + diff;
			}else{
			v[i] = diff;
			} // else
		} // for

		return v;
    } // invariant

//----------------------------------------------------------------------------------------------------
	/**
	Escolhe o ponto inicial do codigo da cadeia (ja invariante a rotacao).
	*/
   public static int[] initialPoint(int[] vet){
        
        int[] v = new int[vet.length];
        int m = 7; // for the minimum
        int max = 1; // max of repetitions
        int count = 0;
        int j = 0;
        int index = 0; // for the index of the minimum
        
		for (int i=0; i<vet.length; i++){ // pecorre o vetor para achar o menor valor
            m = Math.min(vet[i], m);
        }
		
        for (int i=0; i < vet.length; i++){ // acha a maior streak
            if(vet[i] == m){
                count++;
            } else{
                if(count > max){
                    max = count;
                    index = i;
                } // if
                count = 0;
            }// else
        }// for
        
		for (int i=index-max; i < vet.length; i++){ // preenche o vetor novo começando pela streak
            v[j] = vet[i];
            j++;
        } // for
		
        for (int i=0; i< index-max; i++){ // termina de completar o vetor
            v[j] = vet[i];
            j++;
        }
		
        return v;
    } // initialPoint

//----------------------------------------------------------------------------------------------------
	/**
	Calcula a frequencia direcional para dado codigo da cadeia.
	@param vet	vetor resultante da chain code
	*/
	public static float[] freqDir(int[] vet){
		float[] freq = new float[8];
		
		//inicializa o vetor de frequencias
		for(int i = 0; i< 8; i++){
			freq[i] = 0;
		}

		//Faz a contagem dos valores
		for(int i = 0; i< vet.length; i++){
			freq[vet[i]]++;
		}
		
		//normaliza
		for(int i = 0; i< 8; i++){
			freq[i] = freq[i]/vet.length;
		}
		
		return freq;	
	}
	
}