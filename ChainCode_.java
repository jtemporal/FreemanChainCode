/*
Jessica Temporal			Nº USP: 7547611
Matheus Calil Faleiros		Nº USP: 8505053
Natália Baptista Cruz		Nº USP: 4624319
Vitor Valsichi Cuziol		Nº USP: 8505081
*/

import java.io.*;
import java.text.DecimalFormat;
import java.lang.Math;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;


/** 
Interface para o ImageJ 
*/
public class ChainCode_ {

	public ChainCode_() {
	
			// 1. Escolha da imagem de referencia
			String[] refImgPath = referenceImg();
			
			// 2. Escolha do diretorio com as imagens de busca
			String[][] files = getImages(refImgPath);
			
			// 3. Recebe o parametro (taxa de reamostragem); 
			// o valor maximo da taxa eh o tamanho da imagem referencia
			ImagePlus image = new Opener().openImage(refImgPath[0], refImgPath[1]);
			ImageProcessor ip = image.getProcessor();
			int max = Math.min(ip.getWidth(), ip.getHeight());
			int rate = parameter(max);
			
			// 4. Gera os vetores para cada imagem
			String codes = processImages(files, rate);
			
			// 5. Cria o arquivo com os vetores de caracteristica
			writeFile(codes);
			
	}
//----------------------------------------------------------------------------------------------------------------
	/**
	Abre uma imagem de referencia e retorna o caminho.
	*/
	public String[] referenceImg() {
		
		// dialogo para abertura da imagem
		OpenDialog od = new OpenDialog("Open reference image", "", "");
		String[] file_path = {od.getDirectory(), od.getFileName()};
		
		if (file_path[0] == null) {
			return null;
		}
		
		return file_path;
		
	}
//----------------------------------------------------------------------------------------------------------------
	/**
	Obtem imagens do diretorio escolhido pelo usuario.
	@return files		Vetor de imagens encontradas no diretorio.
	*/
	public String[][] getImages(String[] refImgPath) {
		
		// dialog para selecionar diretorio
		DirectoryChooser sd = new DirectoryChooser("Choose folder with images");
		
		// caminho para o diretorio
		String dir = sd.getDirectory();
		
		// se diretorio for vazio
		if (dir == null) {
			return null;
		}

		IJ.log("Searching images: " + sd.getDirectory());

		// adiciona barra no string do diretorio
		if( !dir.endsWith(File.separator) ) {
			dir += File.separator;
		}
		
		// gera lista com nomes das imagens naquele diretorio
		String[] list = new File(dir).list();
		
		if(list == null) {
			return null;
		}

		String[][] files = new String[2][list.length+1];
		
		// o primeiro arquivo em files eh a imagem de referencia.
		files[0][0] = refImgPath[0];
		files[1][0] = refImgPath[1];
		
		// adiciona diretorio e arquivos na lista
		for (int i = 0; i < list.length; i++) {
			files[0][i+1] = dir;
			files[1][i+1] = list[i];
		}

		return files;
	}

//----------------------------------------------------------------------------------------------------------------	
	private static int parameter(int max) {
		
		GenericDialog gd = new GenericDialog("Chain code");
		
		gd.addNumericField("Resampling rate: ", 2, 0);
		gd.showDialog();
		
		if (gd.wasCanceled()) 
			return -1;
		
		int rate = (int)gd.getNextNumber();
		
		if (rate < 0|| rate > max + 1) 
			return -2;
		
		return rate;
	}
//----------------------------------------------------------------------------------------------------------------	
	/** 
	Gera codigo da cadeia para cada imagem dada.
	@param files		Arquivos a serem processados.
	*/
	public String processImages(String[][] files, int rate) {
	
		// string para o qual vao ser jogados as caracteristicas
		String text = "";
	
		if (files == null) {
			return null;
		}

		IJ.log("Processing");
		
		// percorre os arquivos gerando o codigo para cada um
		for (int i = 0; i < files[0].length; i++) {
		
			// mostra progresso na janela do ImageJ
			IJ.showStatus(i + "/" + files[0].length + ": " + files[1][i]); 
			IJ.showProgress( (double)(i/files.length) );
			
			File file = new File(files[0][i] + files[1][i]);//diretorio + nome do arquivo
			
			if (!file.isDirectory()) {
				
				// abre imagem
				text += files[1][i] + ":";
				ImagePlus image = new Opener().openImage( files[0][i], files[1][i] );

				// gera vetor de atributos e retorna string que vai ser escrito no arquivo
				if (image != null) {
					
					ImageAccess img = new ImageAccess(image.getProcessor());
					
					// todo o processo de geracao do codigo da cadeia
					ImageAccess resampledImg = ChainCode.resample(img, rate);
					int[][] s = ChainCode.outlinePixels(resampledImg);
					ImageAccess outlineImg = ChainCode.outlineImage(s, resampledImg.getWidth(), resampledImg.getHeight());
					int[] tempCode = ChainCode.chainCode(outlineImg, s);
					tempCode = ChainCode.invariant(tempCode);
					int[] code = ChainCode.initialPoint(tempCode);

					// calculo da frequencia direcional
					float[] freq = ChainCode.freqDir(code);
					
					// joga todos os atributos do vetor no string
					for (int j = 0; j < freq.length; j++) {
						text += " " + freq[j];
					}
					
					text += "\n";
				}
			}
		}

		return text;
	}
//----------------------------------------------------------------------------------------------------------------		
	/**
	Escreve o texto dado em um arquivo.
	@param text		Texto a ser escrito no arquivo.
	*/
	public void writeFile(String text) {
		
		// abertura do dialog para salvar arquivo
		SaveDialog sd = new SaveDialog("", ".txt", ".txt");
		String file = sd.getDirectory() + "/" + sd.getFileName();

		// confere se arquivo eh valido
		if(file == null) {
			return;
		}

		IJ.log("Saving text to file: " + file);

		// escreve arquivo
		try {
			PrintWriter pw = new PrintWriter(file, "UTF-8");
			pw.println(text);
			pw.close();
		} catch(IOException exception) {
			IJ.log(exception.getMessage());
			return;
		}

		IJ.log("* Text saved.");
		
	}
}
