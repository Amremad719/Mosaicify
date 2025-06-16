# Mosaicify

**Mosaicify** is a Java-based desktop application that generates photomosaics — large images created by arranging many smaller images (tiles) — using a customizable library of sub-images. It provides a simple and intuitive GUI for users to select an image, adjust mosaic parameters, and produce high-quality photomosaics.

<p align="center">
  <img src="https://github.com/Amremad719/Mosaicify/blob/main/Screenshots/Mosaicify.png" width="600"/>
</p>

## ✨ Features

- Generate photomosaics from any source image.
- Use a custom directory of sub-images as the tile library.
- Adjustable mosaic granularity:
  - **Sub-image kernel size** (tile resolution)
  - **Mosaic subdivision count** (tiles across width and height)
- Optional output resolution control.
- Clean and responsive JavaFX interface.
- High-performance image processing using OpenCV.

## 🛠️ Built With

- **Java 17+**
- **JavaFX** – GUI framework
- **OpenCV** – Image processing and analysis
- **Maven** – Build automation and dependency management

## 📸 How It Works

1. **Set Sub-image Library** – Choose a folder containing the image tiles to use in the mosaic.
2. **Set Source Image** – Select the image you want to mosaicify.
3. **Adjust Parameters** – Define:
   - Tile size (kernel width/height)
   - Mosaic grid size (number of subdivisions)
   - Optional output resolution
4. **Generate Mosaic** – Click the button and watch Mosaicify build your photomosaic.

## 🧩 Example Use Case

> Create a mosaic of a portrait using hundreds of images from your personal gallery or movie frames.

## 📦 Getting Started

### Prerequisites

- Java 17 or later
- Maven
- OpenCV 4.x installed and properly linked

### Clone and Build

```bash
git clone https://github.com/Amremad719/Mosaicify.git
cd Mosaicify
mvn clean install
