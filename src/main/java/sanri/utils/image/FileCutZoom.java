package sanri.utils.image;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import sanri.utils.RegexValidate;
import sanri.utils.image.ImageFileConfig.ImageType;
import sanri.utils.image.ImageFileConfig.PathPolicy;
import sanri.utils.image.ImageFileConfig.PicNameRenamePolicy;

public class FileCutZoom {
	public static Integer getRoundIntFromDouble(Double doubleValue) {
		return Integer.parseInt(String.valueOf(Math.round(doubleValue)));
	}

	/**
	 * 
	 * 功能:裁剪图片<br/>
	 * 创建时间:2016-5-31下午8:19:12<br/>
	 * 作者：sanri<br/>
	 * 入参说明:图片文件,裁剪后的图片配置<br/>
	 * 出参说明：<br/>
	 * 
	 * @throws IOException
	 * <br/>
	 */
	public static Map<String, BufferedImage> cutImage(File srcPic, ImageFileConfig config) throws IOException {
		if (!srcPic.exists()) {
			throw new IOException("源图片文件不存在");
		}
		FileInputStream inputStream = new FileInputStream(srcPic);
		String fileName = srcPic.getName();
		return cutImage(inputStream, fileName, config);
	}

	public static Map<String, BufferedImage> cutImage(BufferedImage bufferedImage, ImageFileConfig config) {
		BufferedImage subimage = bufferedImage.getSubimage(config.getX(), config.getY(), config.getWidth(), config.getHeight());
		Map<String, BufferedImage> cutImageMap = new HashMap<String, BufferedImage>();
		cutImageMap.put(ImageFileConfig.CUT, subimage);
		if (config.isSaveNature()) {
			cutImageMap.put(ImageFileConfig.NATURE, bufferedImage);
		}
		return cutImageMap;
	}

	/**
	 * 
	 * 功能:裁剪图片<br/>
	 * 创建时间:2016-5-31下午8:20:13<br/>
	 * 作者：sanri<br/>
	 * 入参说明:图片输入流,图片原文件名,裁剪后的图片配置<br/>
	 * 出参说明：<br/>
	 * 
	 * @throws IOException
	 * <br/>
	 */
	public static Map<String, BufferedImage> cutImage(InputStream picStream, String fileName, ImageFileConfig config) throws IOException {
		// 得到文件后缀
		String suffix = FilenameUtils.getExtension(fileName);
		if (StringUtils.isBlank(suffix)) {
			suffix = ImageType.PNG.getSuffix();
		} else {
			suffix = suffix.substring(1);
		}
		if (config.getSuffix() == null) {
			// 如果没有设置生成文件后缀,则采用默认后缀为原文件后缀
			config.setSuffix(ImageType.findTypeBySuffix(suffix));
		}
		// 得到 imageReader
		Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName(suffix);
		ImageReader imageReader = imageReaders.next();
		// 得到图片输入源
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(picStream);
		// 设置输入源
		imageReader.setInput(imageInputStream, true);
		// 设置读取参数
		ImageReadParam defaultReadParam = imageReader.getDefaultReadParam();
		Rectangle sourceRegion = new Rectangle(config.getX(), config.getY(), config.getWidth(), config.getHeight());
		defaultReadParam.setSourceRegion(sourceRegion);
		// 读取到内存,成为 bufferedImage
		BufferedImage bufferedImage = imageReader.read(0, defaultReadParam);

		Map<String, BufferedImage> bufferedImageMap = new HashMap<String, BufferedImage>();
		bufferedImageMap.put(ImageFileConfig.CUT, bufferedImage);
		if (config.isSaveNature()) {
			// 如果需要保存原图,则加入原图
			// 不知道是流关闭了还是咋回事 这里用 ImageIO.read 读不到
			// bufferedImageMap.put(ImageFileConfig.NATURE,
			// ImageIO.read(picStream));
			// bufferedImageMap.put(ImageFileConfig.NATURE,
			// ImageIO.read(imageInputStream));
			BufferedImage natureImage = imageReader.read(0);
			bufferedImageMap.put(ImageFileConfig.NATURE, natureImage);
		}
		return bufferedImageMap;
	}

	/**
	 * 
	 * 功能:将 bufferedImage 进行缩放<br/>
	 * 创建时间:2016-5-31下午8:38:52<br/>
	 * 作者：sanri<br/>
	 * 入参说明:bufferedImage,缩放后的图片配置;通过 ImageIO.read(File|InputStream) 可以得到
	 * bufferedImage<br/>
	 * 出参说明：<br/>
	 * 这个方法色彩严重失真 使用 zoomImageNew 方法代替这个方法
	 * 
	 * @return<br/>
	 */
	@Deprecated
	public static Map<String, BufferedImage> zoomImage(BufferedImage bufferedImage, ImageFileConfig config) {
		double aspectRatio = config.getAspectRatio();
		int picNum = config.getPicNum();
		if (picNum == 0) {
			throw new IllegalArgumentException("请输入要缩放出几张图");
		}
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		Map<String, BufferedImage> bufferedImageMap = new HashMap<String, BufferedImage>();
		for (int i = 0; i < picNum; i++) {
			// 计算新图片宽高
			int newWidth = getRoundIntFromDouble(width * (aspectRatio / (i + 1)));
			int newHeight = getRoundIntFromDouble(height * (aspectRatio / (i + 1)));
			// 缩放
			Image scaledInstance = bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
			bufferedImageMap.put(ImageFileConfig.ZOOMPREFIX + i, toBufferedImage(scaledInstance));
		}
		if (config.isSaveNature()) {
			bufferedImageMap.put(ImageFileConfig.NATURE, bufferedImage);
		}
		return bufferedImageMap;
	}

	public static Map<String, BufferedImage> zoomImageNew(BufferedImage bufferedImage, ImageFileConfig config) {
		double aspectRatio = config.getAspectRatio();
		int picNum = config.getPicNum();
		if (picNum == 0) {
			throw new IllegalArgumentException("请输入要缩放出几张图");
		}
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		Map<String, BufferedImage> bufferedImageMap = new HashMap<String, BufferedImage>();

		for (int i = 0; i < picNum; i++) {
			// 计算新图片宽高
			int newWidth = getRoundIntFromDouble(width * (aspectRatio / (i + 1)));
			int newHeight = getRoundIntFromDouble(height * (aspectRatio / (i + 1)));
			// 缩放 需保证背景透明 
//			BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
//			result.getGraphics().drawImage(bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
//			bufferedImageMap.put(ImageFileConfig.ZOOMPREFIX + i, toBufferedImage(result));
			Graphics2D createGraphics = bufferedImage.createGraphics();
			BufferedImage createCompatibleImage = createGraphics.getDeviceConfiguration().createCompatibleImage(newWidth, newHeight, Transparency.TRANSLUCENT);
			createGraphics.dispose();
			createGraphics = createCompatibleImage.createGraphics();
			createGraphics.drawImage(bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
			bufferedImageMap.put(ImageFileConfig.ZOOMPREFIX + i, toBufferedImage(createCompatibleImage));
		}
		if (config.isSaveNature()) {
			bufferedImageMap.put(ImageFileConfig.NATURE, bufferedImage);
		}
		return bufferedImageMap;
	}

	/**
	 * 
	 * 功能:将 Image 转成 BufferedImage<br/>
	 * 创建时间:2016-5-31下午8:50:07<br/>
	 * 作者：sanri<br/>
	 * 入参说明:<br/>
	 * 出参说明：<br/>
	 * 
	 * @param image
	 * @return<br/>
	 */
	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}
		image = new ImageIcon(image).getImage();
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			int transparency = Transparency.TRANSLUCENT;
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
		}
		if (bimage == null) {
			int type = BufferedImage.TYPE_INT_RGB;
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}
		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimage;
	}

	/**
	 * 
	 * 功能:将 bufferedImage 输出到文件,这里不做输出到流的处理,因为文件数很有可能大于 2 ,如果需要,自己调用
	 * ImageIO.write() 方法<br/>
	 * 创建时间:2016-5-31下午8:51:26<br/>
	 * 作者：sanri<br/>
	 * 入参说明:<br/>
	 * 出参说明：<br/>
	 * 
	 * @return<br/>
	 * @throws IOException
	 */
	public static Map<String, File> write(Map<String, BufferedImage> bufferedImageMap, ImageFileConfig config) throws IOException {
		PicNameRenamePolicy renamePolicy = config.getRenamePolicy();
		PathPolicy pathPolicy = config.getPathPolicy();

		if (renamePolicy == null)
			renamePolicy = config.new DefaultReNamePolicy();
		if (pathPolicy == null)
			pathPolicy = config.new DefaultPathPolicy();
		Map<String, File> destFiles = new HashMap<String, File>();
		if (!RegexValidate.isEmpty(bufferedImageMap)) {
			Iterator<Entry<String, BufferedImage>> bufferedImageIt = bufferedImageMap.entrySet().iterator();
			while (bufferedImageIt.hasNext()) {
				Entry<String, BufferedImage> bufferedImageEntry = bufferedImageIt.next();
				String mark = bufferedImageEntry.getKey();
				BufferedImage bufferedImage = bufferedImageEntry.getValue();

				// 文件名,路径,后缀
				String fileName = renamePolicy.handler(mark);
				ImageType suffix = config.getSuffix();
				File dir = pathPolicy.handler(mark);

				File destFile = new File(dir, fileName + "." + suffix.getSuffix());
				ImageIO.write(bufferedImage, config.getSuffix().getSuffix(), destFile);

				System.out.println(mark + "写出文件路径:" + destFile);
				destFiles.put(mark, destFile);
			}
		}
		return destFiles;
	}

	/**
	 * 
	 * 功能：旋转图片,需指定角度 <br/>
	 * 输入参数：<br/>
	 * 输出参数：<br/>
	 * 作者:sanri<br/>
	 * 创建时间：2016年6月1日下午2:10:01<br/>
	 * 背景不透明 使用 rotateImageNew
	 */
	@Deprecated
	public static Map<String, BufferedImage> rotateImage(BufferedImage src, ImageFileConfig config) {
		int srcWidth = src.getWidth(null);
		int srcHeight = src.getHeight(null);
		if (config.getSuffix() == null) {
			throw new IllegalArgumentException("先旋转图片需先设置后缀");
		}
		int angel = config.getRotate();
		// calculate the new image size
		Rectangle rectDes = calcRotatedSize(new Rectangle(new Dimension(srcWidth, srcHeight)), angel);

		BufferedImage resultBufferedImage = new BufferedImage(rectDes.width, rectDes.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resultBufferedImage.createGraphics();
		// transform
		g2.translate((rectDes.width - srcWidth) / 2, (rectDes.height - srcHeight) / 2);
		g2.rotate(Math.toRadians(angel), srcWidth / 2, srcHeight / 2);

		g2.drawImage(src, null, null);
		Map<String, BufferedImage> imageMap = new HashMap<String, BufferedImage>();
		imageMap.put(ImageFileConfig.ROTATE, resultBufferedImage);
		if (config.isSaveNature()) {
			imageMap.put(ImageFileConfig.NATURE, src);
		}
		return imageMap;
	}

	/**
	 * 
	 * 功能：产生背景透明的图片,还没搞定<br/>
	 * 输入参数：<br/>
	 * 输出参数：<br/>
	 * 作者:sanri<br/>
	 * 创建时间：2016年6月1日下午3:31:00<br/>
	 */
	public static Map<String, BufferedImage> rotateImageNew(BufferedImage src, ImageFileConfig config) {
		int srcWidth = src.getWidth(null);
		int srcHeight = src.getHeight(null);
		if (config.getSuffix() == null) {
			throw new IllegalArgumentException("先旋转图片需先设置后缀");
		}
		int angel = config.getRotate();
		// calculate the new image size
		Rectangle rectDes = calcRotatedSize(new Rectangle(new Dimension(srcWidth, srcHeight)), angel);

		Graphics2D graphics = src.createGraphics();
		BufferedImage resultBufferedImage = graphics.getDeviceConfiguration().createCompatibleImage(rectDes.width, rectDes.height, Transparency.TRANSLUCENT);
		graphics.dispose();
		// 得到新图的画笔
		graphics = resultBufferedImage.createGraphics();
		graphics.setStroke(new BasicStroke(1));
		// transform
		graphics.translate((rectDes.width - srcWidth) / 2, (rectDes.height - srcHeight) / 2);
		graphics.rotate(Math.toRadians(angel), srcWidth / 2, srcHeight / 2);

		graphics.drawImage(src, null, null);
		Map<String, BufferedImage> imageMap = new HashMap<String, BufferedImage>();
		imageMap.put(ImageFileConfig.ROTATE, resultBufferedImage);
		if (config.isSaveNature()) {
			imageMap.put(ImageFileConfig.NATURE, src);
		}
		return imageMap;
	}

	/**
	 * 
	 * 功能：根据需要旋转的角度计算旋转后的尺寸<br/>
	 * 输入参数：<br/>
	 * 输出参数：<br/>
	 * 作者:sanri<br/>
	 * 创建时间：2016年6月1日下午2:10:43<br/>
	 */
	public static Rectangle calcRotatedSize(Rectangle src, int angel) {
		// if angel is greater than 90 degree, we need to do some conversion
		if (angel >= 90) {
			if (angel / 90 % 2 == 1) {
				int temp = src.height;
				src.height = src.width;
				src.width = temp;
			}
			angel = angel % 90;
		}

		double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
		double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
		double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
		double angel_dalta_width = Math.atan((double) src.height / src.width);
		double angel_dalta_height = Math.atan((double) src.width / src.height);

		int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
		int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
		int des_width = src.width + len_dalta_width * 2;
		int des_height = src.height + len_dalta_height * 2;
		return new java.awt.Rectangle(new Dimension(des_width, des_height));
	}

	public static void main(String[] args) {
		ImageFileConfig config = new ImageFileConfig();
		File srcPic = new File("f:/picture.jpg");
		try {
			config.setX(132);
			config.setY(205);
			config.setWidth(283);
			config.setHeight(159);
			config.setAspectRatio(1 / 4.19);
			config.setPicNum(3);
			config.setRotate(45);
			config.setSaveNature(true);
			config.setSuffix(ImageType.PNG);
			BufferedImage nature = ImageIO.read(srcPic);
			Map<String, BufferedImage> rotateImage = FileCutZoom.rotateImageNew(nature, config);
			FileCutZoom.write(rotateImage, config);
			config.setSaveNature(false);
			BufferedImage bufferedImage = rotateImage.get(ImageFileConfig.ROTATE);
			Map<String, BufferedImage> cutImage = FileCutZoom.cutImage(bufferedImage, config);
			FileCutZoom.write(cutImage, config);
			Map<String, BufferedImage> zoomImage = FileCutZoom.zoomImageNew(bufferedImage, config);
			FileCutZoom.write(zoomImage, config);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
