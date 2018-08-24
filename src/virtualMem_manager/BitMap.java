package virtualMem_manager;

public class BitMap
{
	protected int[] bitmap = new int[32];
	
	protected void init()
	{
		bitmap = new int[32];
	}
	
	// Assign a free frame of the given size and set the associated bitmap
	protected int assign_frame(int index, int size)
	{
		int frameIndex = available_frame(index, size);
		
		if(frameIndex < 0)
		{
			System.out.println("Frame not available.");
			return -1;
		}
		
		for (int i = 0; i < size; i++)
			set_bitmap(frameIndex + i);

		return frameIndex;
	}

	// Check if the frame at the given index and size is available
	private int available_frame(int index, int size)
	{
		int bitMask, bIndex;
		boolean free;
		
		free = true;
		for (int j = 0; j < size; j++) 
		{
			bitMask = (index + j) % 32;
			bIndex = (index + j) / 32;
			if ((bitmap[bIndex] & VMM.MASK[bitMask]) != 0)
				free = false;
		}
		if (free)
			return index;
		return -1;
	}
	
	// Assign a free frame of the given size and set the associated bitmap
	protected int assign_frame(int size)
	{
		int frameIndex = available_frame(size);
		
		for(int i = 0; i < size; i++)
			set_bitmap(frameIndex+i);
		
		return frameIndex;
	}
	
	// find the first available frame of the given size, do not set bitmap
	private int available_frame(int size)
	{
		int bitMask, index;
		boolean free;

		// index 0 is reserved to the segment table
		for (int i = 1; i <= 1024-size; i++) 
		{
			free = true;
			for(int j = 0; j < size; j ++)
			{
				bitMask = (i+j) % 32;
				index = (i+j) / 32;
				if ((bitmap[index] & VMM.MASK[bitMask]) != 0)
					free = false;
			}
			if(free)
				return i;
		}
		throw new RuntimeException("No frame available.");
	}
	
	// Set bitmap bitNumb to used (bit = 1)
	protected void set_bitmap(int bitNumb)
	{
		check_valid_bitmap_value(bitNumb);
		int index = bitNumb / 32;
		int bitMask = bitNumb % 32;

		bitmap[index] = bitmap[index] | VMM.MASK[bitMask];
	}

	// Set bitmap bitNumb to not used (bit = 0)
	protected void unset_bitmap(int bitNumb)
	{
		check_valid_bitmap_value(bitNumb);
		int index = bitNumb / 32;
		int bitMask = bitNumb % 32;

		bitmap[index] = bitmap[index]  & ~VMM.MASK[bitMask];
	}
	
	private static void check_valid_bitmap_value(int n)
	{
		if(n < 0 || n > 1024)
			throw new IndexOutOfBoundsException("Invalid bitmap value: " + n);
	}

}
